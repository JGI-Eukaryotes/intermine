package org.intermine.objectstore.intermine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.Util;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseConnectionException;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.ShutdownHook;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.model.FastPathObject;

public class ObjectStoreInterMineRestrictedImpl extends ObjectStoreInterMineImpl {

  private static final Logger LOG = Logger.getLogger(ObjectStoreInterMineRestrictedImpl.class);

  // what and how we restrict
  protected String restrictedClass = null;  // i.e. Organism
  protected String restrictedField = null;  // i.e. taxonId
  protected String restrictedMethod = null; // i.e. cookie
  protected String restrictedKey = null;    // cookie name
  protected String restrictedValue = null;  // cookie value

  protected ObjectStoreInterMineRestrictedImpl(Model model) {
    super(model);
  }

  protected ObjectStoreInterMineRestrictedImpl(Database db, DatabaseSchema schema) {
    super(db,schema);
  }

  protected void setRestrictedClass(String className) {
    restrictedClass = className;
  }
  protected void setRestrictedField(String fieldName) {
    restrictedField = fieldName;
  }
  protected void setRestrictedMethod(String methodName) {
    restrictedMethod = methodName;
  }
  protected void setRestrictedKey(String keyName) {
    restrictedKey = keyName;
  }

  public void initState(HttpServletRequest request) {
    // always reset to null
    restrictedValue = null;
    // only the "cookie" method is implemented now. Other possibilities later?
    if ( restrictedMethod.equals("cookie") ) {
      for (Cookie cookie: request.getCookies()) {
        if ( restrictedKey.equals(cookie.getName()) ) {
          try {
            restrictedValue = URLDecoder.decode(cookie.getValue(),"UTF-8");
          } catch (UnsupportedEncodingException e) {}
          // remove leading and trailing square brackets if present
          if (restrictedValue.startsWith("[") && restrictedValue.endsWith("]")) {
            restrictedValue = restrictedValue.substring(1,restrictedValue.length()-1);
          }
        }
      }
    }
  }
  public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
      boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {

    if (restrictedClass != null && restrictedValue != null) {
      constrainQuery(q);
    }
    return super.execute(q,start,limit,optimise,explain,sequence);
  }
  public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
      boolean prefetch) {

    if (restrictedClass != null && restrictedValue != null) {
      constrainQuery(q);
    }
    return super.execute(q,batchSize,optimise,explain,prefetch);
  }
  public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
      boolean explain, boolean prefetch) {

    if (restrictedClass != null && restrictedValue != null) {
      constrainQuery(q);
    }
    return super.executeSingleton(q,batchSize,optimise,explain,prefetch);
  }
  private void constrainQuery(Query q) {

    // see if the restricted class is in the from. We're going to do this
    // in 2 passes.
    // First, let's see if if the table is referenced directly
    for( FromElement f: q.getFrom()) {
      if ( f instanceof QueryClass) {
        QueryClass fClass = (QueryClass)f;
        if (fClass.toString().equals("org.intermine.model.bio."+restrictedClass)) {
          ConstraintSet cContainer = new ConstraintSet(ConstraintOp.AND);
          ConstraintSet cRestrictions = new ConstraintSet(ConstraintOp.OR);
          QueryField rField = new QueryField(fClass,restrictedField);
          for ( String field: restrictedValue.split(",") ) {
            // cast the field to an integer if we can. Otherwise, it's a string
            QueryValue qValue;
            try {
              qValue = new QueryValue(Integer.parseInt(field));
            } catch (NumberFormatException e) {
              qValue = new QueryValue(field);
            }
            cRestrictions.addConstraint(new SimpleConstraint(rField,ConstraintOp.EQUALS,qValue));
          }
          cContainer.addConstraint(cRestrictions);
          Constraint c = q.getConstraint();
          if (c==null) {
            q.setConstraint(cContainer);
          } else {
            cContainer.addConstraint(c);
            q.setConstraint(cContainer);
          }
        }
      }
    }
    // Next, lets look for everything that references the specific class.
    // This could be redundant (there may already be a FK constraint to the
    // restrictedClass table we processed already. Or not.
    // Some perverse queries may have multiple tables and several of them
    // need separate constraints.
    // we might have to add a new table to the 'from'. Since we're iterating
    // over the from's this can only be done at the end.
    ArrayList<QueryClass> qClasses = new ArrayList<QueryClass>();
    // in some cases we can reuse the table if it's already in the FROMs.
    // but it's difficult to determine if this will work: if the pathquery
    // already has an organism and homolog, we don't know if the organism is
    // tied to the organismid or the orthlogo_organismid of the homolog. If
    // we reuse the organism table, we'll never get the cross terms.
    for( FromElement f: q.getFrom()) {
      if ( f instanceof QueryClass) {
        QueryClass fClass = (QueryClass)f;
        if (!fClass.toString().equals("org.intermine.model.bio."+restrictedClass)) {
          for (ReferenceDescriptor sF : getModel().getClassDescriptorByName(fClass.toString())
              .getAllReferenceDescriptors()) {
            String unqualClassRef = sF.getReferencedClassDescriptor().getUnqualifiedName();
            if (unqualClassRef.equals(restrictedClass)) {
              // we need to add a constraint
              ConstraintSet cContainer = new ConstraintSet(ConstraintOp.AND);
              ConstraintSet cRestrictions = new ConstraintSet(ConstraintOp.OR);
              QueryClass qClass = null;
              try {
                qClass = new QueryClass(Class.forName(sF.getReferencedClassName()));
                qClasses.add(qClass);
              } catch (ClassNotFoundException e1) {
                LOG.error("Cannot load class for "+sF.getReferencedClassName());
                return;
              }
              QueryField rField = new QueryField(qClass,restrictedField);
              for ( String field: restrictedValue.split(",") ) {
                // cast the field to an integer if we can. Otherwise, it's a string
                QueryValue qValue;
                try {
                  qValue = new QueryValue(Integer.parseInt(field));
                } catch (NumberFormatException e) {
                  qValue = new QueryValue(field);
                }
                cRestrictions.addConstraint(new SimpleConstraint(rField,ConstraintOp.EQUALS,qValue));
              }
              QueryObjectReference qoRef = new QueryObjectReference(fClass,sF.getName());
              cContainer.addConstraint(new ContainsConstraint(qoRef,ConstraintOp.CONTAINS,qClass));
              cContainer.addConstraint(cRestrictions);
              Constraint c = q.getConstraint();
              if (c==null) {
                q.setConstraint(cContainer);
              } else {
                cContainer.addConstraint(c);
                q.setConstraint(cContainer);
              }
            }
          }
        }
      }
    }
    // add the new classes.
    for( QueryClass qClass: qClasses) {
      q.addFrom(qClass);
    }
  }


  /**
   * Gets a ObjectStoreInterMineRestrictedImpl instance for the given underlying properties
   *
   * This is copy pasta from ObjectStoreInterMineImpl.getIntance.
   * 
   * @param osAlias the alias of this objectstore
   * @param props The properties used to configure a InterMine-based objectstore
   * @return the ObjectStoreInterMineImpl for this repository
   * @throws IllegalArgumentException if props or model are invalid
   * @throws ObjectStoreException if there is any problem with the instance
   */
  public static ObjectStoreInterMineRestrictedImpl getInstance(String osAlias, Properties props)
      throws ObjectStoreException {
    String dbAlias = props.getProperty("db");
    if (dbAlias == null) {
      throw new ObjectStoreException("No 'db' property specified for InterMine"
          + " objectstore (" + osAlias + ")."
          + "Check properties file");
    }

    // Database-format properties
    String missingTablesString = props.getProperty("missingTables");
    String truncatedClassesString = props.getProperty("truncatedClasses");
    String noNotXmlString = props.getProperty("noNotXml");

    // Non-format properties
    String logfile = props.getProperty("logfile");
    String logTable = props.getProperty("logTable");
    String minBagTableSizeString = props.getProperty("minBagTableSize");
    String logEverythingString = props.getProperty("logEverything");
    String verboseQueryLogString = props.getProperty("verboseQueryLog");
    String logExplainsString = props.getProperty("logExplains");
    String logBeforeExecuteString = props.getProperty("logBeforeExecute");
    String disableResultsCacheString = props.getProperty("disableResultsCache");


    synchronized (instances) {
      ObjectStoreInterMineRestrictedImpl os = (ObjectStoreInterMineRestrictedImpl)instances.get(osAlias);
      if (os == null) {
        Database database;
        try {
          database = DatabaseFactory.getDatabase(dbAlias);
        } catch (Exception e) {
          throw new ObjectStoreException("Unable to get database for InterMine"
              + " ObjectStore", e);
        }
        String versionString = null;
        int formatVersion = Integer.MAX_VALUE;
        try {
          versionString = MetadataManager.retrieve(database,
              MetadataManager.OS_FORMAT_VERSION);
        } catch (DatabaseConnectionException e) {
          throw new ObjectStoreException("Failed to get connection to database while "
              + "instantiating ObjectStore", e);
        } catch (SQLException e) {
          LOG.warn("Error retrieving database format version number", e);
          throw new ObjectStoreException(
              "The table intermine_metadata doesn't exist. Please run build-db");
        }
        if (versionString == null) {
          formatVersion = 0;
        } else {
          try {
            formatVersion = Integer.parseInt(versionString);
          } catch (NumberFormatException e) {
            NumberFormatException e2 = new NumberFormatException("Cannot parse database"
                + " format version \"" + versionString + "\"");
            e2.initCause(e);
            throw e2;
          }
        }
        if (formatVersion > 1) {
          throw new IllegalArgumentException("Database version is too new for this code. "
              + "Please update to a newer version of InterMine. Database version: "
              + formatVersion + ", latest supported version: 1");
        }
        Model osModel;
        try {
          osModel = getModelFromClasspath(osAlias, props);
        } catch (MetaDataException e) {
          throw new ObjectStoreException("Cannot load model", e);
        }
        if (formatVersion >= 1) {
          // If it's version >=1 then ignore the properties, and use the embedded values.
          try {
            truncatedClassesString = MetadataManager.retrieve(database,
                MetadataManager.TRUNCATED_CLASSES);
            missingTablesString = MetadataManager.retrieve(database,
                MetadataManager.MISSING_TABLES);
            noNotXmlString = MetadataManager.retrieve(database,
                MetadataManager.NO_NOTXML);
          } catch (SQLException e) {
            throw new IllegalArgumentException("Couldn't retrieve embedded config "
                + "for ObjectStore " + osAlias);
          }
          if (noNotXmlString == null) {
            throw new IllegalArgumentException("Missing embedded config for ObjectStore"
                + " " + osAlias);
          }
        }
        List<ClassDescriptor> truncatedClasses = new ArrayList<ClassDescriptor>();
        if (truncatedClassesString != null) {
          String[] classes = truncatedClassesString.split(",");
          for (int i = 0; i < classes.length; i++) {
            ClassDescriptor truncatedClassDescriptor =
                osModel.getClassDescriptorByName(classes[i]);
            if (truncatedClassDescriptor == null) {
              throw new ObjectStoreException("Truncated class " + classes[i]
                  + " does not exist in the model");
            }
            truncatedClasses.add(truncatedClassDescriptor);
          }
        }
        boolean noNotXml = false;
        if ("true".equals(noNotXmlString) || (noNotXmlString == null)) {
          noNotXml = true;
        } else if ("false".equals(noNotXmlString)) {
          noNotXml = false;
        } else {
          throw new ObjectStoreException("Invalid value for property noNotXml: "
              + noNotXmlString);
        }
        HashSet<String> missingTables = new HashSet<String>();
        if (missingTablesString != null) {
          String[] tables = missingTablesString.split(",");
          for (int i = 0; i < tables.length; i++) {
            missingTables.add(tables[i].toLowerCase());
          }
        }

        // if we're above Postgres version 9.2 we can use the built-in range types
        boolean useRangeTypes = database.isVersionAtLeast("9.2");

        // Check if there is a bioseg index in the database for faster range queries
        // - if we can use range types we don't really need to check this but useful to know
        boolean hasBioSeg = false;
        Connection c = null;
        try {
          c = database.getConnection();
          Statement s = c.createStatement();
          s.execute("SELECT bioseg_create(1, 2)");
          hasBioSeg = true;
        } catch (DatabaseConnectionException e) {
          throw new ObjectStoreException("Failed to get database connection when checking"
              + " for bioseg during ObjectStore creation", e);
        } catch (SQLException e) {
          // We don't have bioseg
          if (!useRangeTypes) {
            // only log a warning if we can't use range types, otherwise no problem
            LOG.warn("Database " + osAlias + " doesn't have bioseg", e);
          }
        } finally {
          if (c != null) {
            try {
              c.close();
            } catch (SQLException e) {
              // Whoops.
            }
          }
        }

        DatabaseSchema schema = new DatabaseSchema(osModel, truncatedClasses, noNotXml,
            missingTables, formatVersion, hasBioSeg, useRangeTypes);
        os = new ObjectStoreInterMineRestrictedImpl(database, schema);
        os.description = osAlias;

        // set the restriction parameters  
        os.setRestrictedClass(props.getProperty("restrictedClass"));
        os.setRestrictedField(props.getProperty("restrictedField"));
        os.setRestrictedMethod(props.getProperty("restrictedMethod"));
        os.setRestrictedKey(props.getProperty("restrictedKey"));

        if (logfile != null) {
          try {
            FileWriter fw = new FileWriter(logfile, true);
            BufferedWriter logWriter = new BufferedWriter(fw);
            ShutdownHook.registerObject(logWriter);
            os.setLog(logWriter);
          } catch (IOException e) {
            LOG.warn("Error setting up execute log in file " + logfile + ": " + e);
          }
        }
        if (logTable != null) {
          try {
            os.setLogTableName(logTable);
          } catch (SQLException e) {
            LOG.warn("Error setting up execute log in database table " + logTable + ":"
                + e);
          }
        }
        if (minBagTableSizeString != null) {
          try {
            int minBagTableSizeInt = Integer.parseInt(minBagTableSizeString);
            os.setMinBagTableSize(minBagTableSizeInt);
          } catch (NumberFormatException e) {
            LOG.warn("Error setting minBagTableSize: " + e);
          }
        }
        if ("true".equals(logEverythingString)) {
          os.setLogEverything(true);
        }
        if ("true".equals(verboseQueryLogString)) {
          os.setVerboseQueryLog(true);
        }
        if ("true".equals(logExplainsString)) {
          os.setLogExplains(true);
        }
        if ("true".equals(logBeforeExecuteString)) {
          os.setLogBeforeExecute(true);
        }
        if ("true".equals(disableResultsCacheString)) {
          os.setDisableResultsCache(true);
        }
        instances.put(osAlias, os);
      }
      return os;
    }
  }

}
