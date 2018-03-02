package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneShadow;
import org.intermine.model.bio.MSA;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinFamily;
import org.intermine.model.bio.ProteinFamilyHMMScore;
import org.intermine.model.bio.ProteinFamilyMember;
import org.intermine.model.bio.Sequence;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.task.DBDirectDataLoaderTask;

/**
 * 
 * @author
 */
public class ProteinHMMScoreConverter  extends DBDirectDataLoaderTask
{
    // 
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";


    private static final Logger LOG = Logger.getLogger(ProteinHMMScoreConverter.class);
    private Connection connection;
    private static final String ChadoGatheringThresholdTypeId = "39252";
    private static final String ChadoFamilyTypeId = "1991";
    private static final String PACMethodDbId = "180";
    protected String methodIds = null;
    
    private Map<String,InterMineObject> familyMap = new HashMap<String, InterMineObject>();
    private Map<String,InterMineObject> protMap = new HashMap<String, InterMineObject>();


    /**
     * {@inheritDoc}
     */
    public void process() {
        // a database has been initialised from properties starting with db.protein-hmmscore
      
      connection = getConnection();

      preFillC("clusterId",familyMap,ProteinFamily.class);
      System.out.println("got clusters");
      preFillP("secondaryIdentifier",protMap,Protein.class);     
      System.out.println("got proteins");

      ResultSet res = getProteinHMMScores();

      try {
      while( res.next()) {
        String pacId = res.getString("uniquename");
        if (!protMap.containsKey(pacId)) {
          LOG.info("Need to register protein for pac id "+pacId);
          Protein p = getDirectDataLoader().createObject(Protein.class);
          p.setSecondaryIdentifier(pacId);
          getDirectDataLoader().store(p);
          protMap.put(pacId,p);
        }
        String clusterId = res.getString("clusterId");
        if (!familyMap.containsKey(clusterId)) {
          LOG.info("Need to register family for cluster id "+clusterId);
          ProteinFamily p = getDirectDataLoader().createObject(ProteinFamily.class);
          p.setClusterId(Integer.parseInt(clusterId));
          p.setMethodId(Integer.parseInt(res.getString("method_id")));
          getDirectDataLoader().store(p);
          familyMap.put(clusterId,p);
        }
          ProteinFamilyHMMScore proFamilyScore = getDirectDataLoader().createObject(ProteinFamilyHMMScore.class);
          proFamilyScore.setProtein((Protein)protMap.get(pacId));
          proFamilyScore.setProteinFamily((ProteinFamily)familyMap.get(clusterId));
          proFamilyScore.setGatheringThreshold(res.getDouble("gathering_threshold"));
          proFamilyScore.setScore(res.getDouble("score"));
          proFamilyScore.setSignificance(res.getDouble("significance"));
          getDirectDataLoader().store(proFamilyScore);

      }
        } catch (ObjectStoreException e1) {
          throw new BuildException("Trouble creating protein family score: "+e1.getMessage());
        } catch (SQLException e) {
          throw new BuildException("SQL error occurred when processing: "+e.getMessage());
        }
      
    }
    
    public ResultSet getProteinHMMScores() {
      // process data with direct SQL queries on the source database
      ResultSet res = null;
      try {
        Statement stmt = connection.createStatement();
        String query = "SELECT "
            + " grp.uniquename AS clusterId,"
            + " f.uniquename AS uniquename, "
            + " a.rawscore as score, "
            + " a.significance as significance, "
            + " d.accession as method_id, "
            + "(SELECT value from grpprop p where p.grp_id=grp.grp_id and p.type_id="+ChadoGatheringThresholdTypeId+") AS gathering_threshold "
            + " FROM"
            + " grp, feature f, analysisfeaturegrp a, dbxref d "
            + " WHERE "
            + " f.feature_id=a.feature_id AND" 
            + " grp.type_id="+ChadoFamilyTypeId+" AND grp.grp_id=a.grp_id AND"
            + " d.dbxref_id=grp.dbxref_id and d.db_id="+PACMethodDbId+" and accession IN ("+quotedList(methodIds)+")";
        LOG.info("Executing query: "+query);
        res = stmt.executeQuery(query);
      } catch (SQLException e) {
        throw new BuildException("Trouble getting hmmscores: " + e.getMessage());
      }
      return res;
    }
    /*
     * set the list of method ids as a comma-delimited list.
     */
    public void setMethodIds(String inString)
    {
      methodIds = inString;
    }
    public String getMethodIds()
    {
      return methodIds;
    }
    
    /*
     * since method id's are strings in chado, convert the string of integers L,M,N,...
     * with 'L','M','N',... If they're already quoted, this should do no harm.
     */
    
    private String quotedList(String a) {
      String[] methodStrings = a.replaceAll("'","").split(",");
      StringBuffer mSB = new StringBuffer();
      for( String mS: methodStrings) {
        if (mSB.length() > 0) mSB.append(",");
        mSB.append("'").append(mS).append("'");
      }
      return mSB.toString();
    }
    /*
     * prefill a hash of either genes or proteins.
     */
    private void preFillC(String field,Map<String,InterMineObject> map,Class<? extends InterMineObject> objectClass) {
      Query q = new Query();
      QueryClass qC = new QueryClass(objectClass);
      q.addFrom(qC);
      QueryField qFName = new QueryField(qC,field);
      q.addToSelect(qFName);
      q.addToSelect(qC);

      LOG.info("Prefilling ProxyReferences. Query is "+q);
      try {
        Results res = getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
        Iterator<Object> resIter = res.iterator();
        LOG.info("Iterating...");
        while (resIter.hasNext()) {
          @SuppressWarnings("unchecked")
          ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
          Integer clId = (Integer)rr.get(0);
          String name = clId.toString();
          InterMineObject obj = (InterMineObject)rr.get(1);
          map.put(name,obj);
        }
      } catch (Exception e) {
        throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
      }
      LOG.info("Retrieved "+map.size()+" ProxyReferences.");

    }
    private void preFillP(String field,Map<String,InterMineObject> map,Class<? extends InterMineObject> objectClass) {
      Query q = new Query();
      QueryClass qC = new QueryClass(objectClass);
      q.addFrom(qC);
      QueryField qFName = new QueryField(qC,field);
      q.addToSelect(qFName);
      q.addToSelect(qC);

      LOG.info("Prefilling ProxyReferences. Query is "+q);
      try {
        Results res = getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
        Iterator<Object> resIter = res.iterator();
        LOG.info("Iterating...");
        while (resIter.hasNext()) {
          @SuppressWarnings("unchecked")
          ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
          String name = (String)rr.get(0);
          InterMineObject obj = (InterMineObject)rr.get(1);
          map.put(name,obj);
        }
      } catch (Exception e) {
        throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
      }
      LOG.info("Retrieved "+map.size()+" ProxyReferences.");

    }
}
