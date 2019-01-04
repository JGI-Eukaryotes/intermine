package org.intermine.bio.dataconversion;


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
public class ChadoFamiliesConverter extends DBDirectDataLoaderTask
{
    // 
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";
    // these are strings since they're part of SQL statements.
    private static final String PACMethodDbId = "180";
    private static final String PACProteomeDbId = "172";
    private static final String ChadoFamilyTypeId = "ANY('{39259,39260}')";
    private static final String ChadoProteinTypeId = "219";
    private static final String ChadoAlignmentTypeId = "39215";
    private static final String ChadoMemberTypeId = "39217";
    private static final String ChadoHmmTypeId = "39216";
    private static final int MaxAlignmentToStore = 1000000;

    private static final Logger LOG = Logger.getLogger(ChadoFamiliesConverter.class);
    private Connection connection;

    protected Map<Integer,ArrayList<Integer>> methodToClusterList = new HashMap<Integer,ArrayList<Integer>>();
    protected String methodIds = null;

    // maps of known genes, proteins, organisms, crossreferences and datassources.
    private Map<String,InterMineObject> geneMap = new HashMap<String, InterMineObject>();
    private Map<String,InterMineObject> protMap = new HashMap<String, InterMineObject>();
    private Map<Integer,Organism> organismHash = new HashMap<Integer,Organism>();
    private Map<String,CrossReference> crossRefHash = new HashMap<String,CrossReference>();
    private Map<String,DataSource> dataSourceHash = new HashMap<String,DataSource>();
    // a set of already entered clusters for this method
    private Set<Integer> alreadyGot = new HashSet<Integer>();

 
 
    /**
     * {@inheritDoc}
     */
    public void process() {
        
        connection = getConnection();

        preFill(geneMap,Gene.class);
        preFill(protMap,Protein.class);
        preFillOrganism();
        preFillClusters();
        
        ResultSet res = getProteinFamilies();

        int ctr = 0;
        try {
          while (res.next()) {
            Integer clusterId = null;
            try {
              clusterId = Integer.parseInt(res.getString("clusterId"));
            } catch (NumberFormatException e) {
              throw new BuildException("clusterId is not a simple integer");
            }
            if (clusterId != null ) {
            // process hmm
            String hmmString = res.getString("hmm");
            String msaString = getMSA(clusterId);

            ProteinFamily proFamily;
            try {
              proFamily = getDirectDataLoader().createObject(ProteinFamily.class);

            } catch (ObjectStoreException e1) {
              throw new BuildException("Trouble creating protein family: "+e1.getMessage());
            }
            Integer methodId = res.getInt("methodId");
            if (methodId != null ) proFamily.setMethodId(methodId);
            if (res.getString("methodName") != null ) proFamily.setMethodName(res.getString("methodName"));

            // this is a list of family members we will store. Separated by proteomeId.
            HashMap<Integer,ArrayList<ProteinFamilyMember>> thingsToStore = 
                           new HashMap<Integer, ArrayList<ProteinFamilyMember>>();
            
            int numMembers = 0;
            try {
              numMembers = registerProteins(clusterId,proFamily,thingsToStore);
            } catch (Exception e1) {
              throw new BuildException("Problem trying to register proteins: "+e1.getMessage());
            }

            String clusterName = res.getString("clusterName");
            if (clusterName != null && clusterName.equals(res.getString("clusterId")) ) {
              clusterName = "Family "+clusterId.toString()+": "+numMembers+" members";
            }
            if (clusterName != null && !clusterName.trim().isEmpty() )
                                    proFamily.setClusterName(clusterName.trim());
            if (clusterId != null ) proFamily.setClusterId(clusterId);

            if (msaString != null || hmmString != null) {
              String newMSA = reformatMSA(msaString);
              String newHMM = reformatHMM(hmmString);
              MSA msa;
              try {
                msa = getDirectDataLoader().createObject(MSA.class);
              } catch (ObjectStoreException e1) {
                throw new BuildException("Trouble when creating MSA: "+e1.getMessage());
              }
              msa.setPrimaryIdentifier("Cluster "+clusterId+" alignment");
              if (newMSA != null) msa.setAlignment(newMSA);
              if (newHMM != null) msa.sethMM(newHMM);
              try {
                getDirectDataLoader().store(msa);
              } catch (ObjectStoreException e) {
                throw new BuildException("Problem storing MSA: " + e.getMessage());
              }
              proFamily.setMsa(msa);
            }
            try {
              registerCrossReferences(clusterId,proFamily);
            } catch (Exception e1) {
              throw new BuildException("Problem registering xrefs " + e1.getMessage());
            }

            try {
              getDirectDataLoader().store(proFamily);
             // ProxyReference pFRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
             //         proFamily.getId(),ProteinFamily.class);
              ctr++;
              if (ctr%10000 == 0) {
                LOG.info("Stored "+ctr+" clusters...");
              }
              for( Integer protId : thingsToStore.keySet() ) {
                ArrayList<ProteinFamilyMember> pfmByOrganism = thingsToStore.get(protId);
                for(ProteinFamilyMember pfm : pfmByOrganism ) {
                  pfm.setProteinFamily(proFamily);
                  getDirectDataLoader().store(pfm);
                }
              }
            } catch (ObjectStoreException e) {
              LOG.error("ObjectStoreException: "+e.getMessage());
              throw new BuildException("Problem storing protein family." + e.getMessage());
            }
          }
          }
        } catch (SQLException e) {
          throw new BuildException("There was an SQL exception: "+e.getMessage());
        }
     
        LOG.info("Stored "+ctr+" clusters.");
      }

      int registerProteins(Integer clusterId, ProteinFamily family,
          HashMap<Integer,ArrayList<ProteinFamilyMember>> thingsToStore) throws Exception{
        ResultSet res = getFamilyMembers(clusterId);
        
        int numMembers = 0;
        while( res.next()) {
          Integer proteomeId = res.getInt("proteomeid");
          numMembers++;

          String pacID = res.getString("transcriptId");
          if (!protMap.containsKey(pacID)) {
            LOG.info("Need to register protein for pac id "+pacID);
            Protein p = getDirectDataLoader().createObject(Protein.class);
            p.setSecondaryIdentifier(pacID);
            p.setOrganism(getOrganism(proteomeId));
            getDirectDataLoader().store(p);
            protMap.put(pacID,p);
          }
          if (!geneMap.containsKey(pacID)) {
            LOG.info("Need to register gene for pac id "+pacID);
            Gene g = getDirectDataLoader().createObject(Gene.class);
            g.setSecondaryIdentifier(pacID);
            g.setOrganism(getOrganism(proteomeId));
            getDirectDataLoader().store(g);
            geneMap.put(pacID,g);
          }
          GeneShadow g = new GeneShadow();
          g.setId(geneMap.get(pacID).getId());
          g.setOrganism(getOrganism(proteomeId));
          g.setSecondaryIdentifier(pacID);
          family.addGene(g);
          ProteinFamilyMember pfm = getDirectDataLoader().createObject(ProteinFamilyMember.class);
          pfm.setMembershipDetail(res.getString("value"));
          LOG.info("Adding member "+pacID+" with proteome id "+proteomeId);
          pfm.setOrganism(getOrganism(proteomeId));
          pfm.setProtein((Protein)protMap.get(pacID));
          pfm.setProteinFamily(family);
     
          family.addMember(pfm);
          if (!thingsToStore.containsKey(proteomeId)) {
            thingsToStore.put(proteomeId,new ArrayList<ProteinFamilyMember>());
          }
          thingsToStore.get(proteomeId).add(pfm);

        }
        res.close();
        // register the organism counts
        Integer memberCount = new Integer(0);
        for( Integer proteomeId : thingsToStore.keySet()) {
          memberCount += thingsToStore.get(proteomeId).size();
        }
        for( Integer protId : thingsToStore.keySet() ) {
          ArrayList<ProteinFamilyMember> pfmByOrganism = thingsToStore.get(protId);
          Integer nMembers = new Integer(pfmByOrganism.size());
          for(ProteinFamilyMember pfm : pfmByOrganism ) {
            pfm.setCount(nMembers);
          }
        }
        family.setMemberCount(memberCount);
        return numMembers;
      }

      protected Sequence storeSequence(String residues)  throws ObjectStoreException {
        if ( residues.length() == 0) {
          return null;
        }
        MessageDigest md;
        try {
          md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
          throw new BuildException("No such algorithm for md5?");
        }
        Sequence sequence = getDirectDataLoader().createObject(Sequence.class);
        md.update(residues.getBytes(),0,residues.length());
        String md5sum = new BigInteger(1,md.digest()).toString(16);
        sequence.setMd5checksum(md5sum);
        sequence.setLength(new Integer(residues.length()));
        sequence.setResidues(new PendingClob(residues));
        try {
          getDirectDataLoader().store(sequence);
          return sequence;
          //return new Reference(getIntegrationWriter().getObjectStore(),sequence.getId(),Sequence.class);
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem storing sequence." + e.getMessage());
        }
      }

      public String getPeptideSequence(String transcriptId) {
        ResultSet res = null;
        try {
          Statement stmt = connection.createStatement();
          String query = "SELECT residues FROM "
              + " feature"
              + " WHERE uniquename='PAC:"+transcriptId+"'"
              + " AND type_id="+ChadoProteinTypeId;
          res = stmt.executeQuery(query);
          // should only have 1 row; we'll return the first non-empty value,
          // but we'll remove terminal stops.
          while( res.next()) {
            String peptide = res.getString("residues");
            if (peptide.endsWith("*")) {
              return peptide.substring(0,peptide.length()-1);
            } else if (!peptide.isEmpty() ) {
              return peptide;
            }
          }

        } catch (SQLException e) {
          throw new BuildException("Trouble getting singleton peptide: " + e.getMessage());
        }
        return null;
      }
      
      public String getMSA(Integer clusterId) {
        ResultSet res = null;
        StringBuffer fasta = new StringBuffer();
        try {
          Statement stmt = connection.createStatement();
          String query = "select "
              + " replace(f.uniquename,'PAC:','') as transcriptId, "
              + " f.name as proteinName, "
              + " d.accession as proteomeId, "
              + " p.value FROM "
              + " grp, grpmember m, feature_grpmember fm, grpmemberprop p,"
              + " feature f, feature_dbxref fd, dbxref d "
              + " where grp.uniquename='"+clusterId+"'"
              + " AND f.feature_id=fd.feature_id"
              + " AND d.dbxref_id=fd.dbxref_id"
              + " AND d.db_id="+PACProteomeDbId+" and grp.type_id="+ChadoFamilyTypeId
              + " AND grp.grp_id=m.grp_id"
              + " AND m.grpmember_id=fm.grpmember_id"
              + " AND m.grpmember_id=p.grpmember_id"
              + " AND f.feature_id=fm.feature_id"
              + " AND p.type_id="+ChadoAlignmentTypeId;
          res = stmt.executeQuery(query);
 
          while ( res.next() ) {
            String header = ">jgi|"+res.getString("transcriptId")+ " "+
                res.getString("proteomeId")+" "+res.getString("proteinName")+"\n";
            if (fasta.length() + header.length() > MaxAlignmentToStore) {
              res.close();
              LOG.info("Alignment is too long for "+clusterId.toString());
              return new String("<Alignment too long to store>");
            }
            fasta.append(header);
            String residues = res.getString("value");
            int i = 0;
            while(i < residues.length()) {
              int toEat = (i+60<residues.length())?60:(residues.length()-i);
              if( fasta.length() + toEat + 1 > MaxAlignmentToStore) {
                res.close();
                LOG.info("Alignment is too long for "+clusterId.toString());
                return new String("<Alignment too long to store>");
              }
              fasta.append(residues.substring(i,i+toEat)+"\\n");
              i+=60;
            }
          }
          res.close();
        } catch (SQLException e) {
          throw new BuildException("Trouble getting family members names: " + e.getMessage());
        }
        return fasta.length()>0?fasta.toString():null;
      }

      public ResultSet getFamilyMembers(Integer clusterId) {
        ResultSet res = null;
        try {
          Statement stmt = connection.createStatement();
          String query = "SELECT f.name as peptideName,"
              + " f.uniquename AS transcriptId, "
              + " d.accession AS proteomeid, "
              + " p.value"
              + " FROM"
              + " grp, grpmember m, feature_grpmember fm, grpmemberprop p,"
              + " feature f, feature_dbxref fd, dbxref d "
              + " WHERE grp.uniquename='"+clusterId+"'" 
              + " AND grp.grp_id=m.grp_id "
              + " AND m.grpmember_id=fm.grpmember_id "
              + " AND f.feature_id=fm.feature_id"
              + " AND f.feature_id=fd.feature_id"
              + " AND d.dbxref_id=fd.dbxref_id"
              + " AND d.db_id="+PACProteomeDbId+" and grp.type_id="+ChadoFamilyTypeId
              + " AND m.grpmember_id=p.grpmember_id"
              + " AND p.type_id="+ChadoMemberTypeId;
          res = stmt.executeQuery(query);
        } catch (SQLException e) {
          throw new BuildException("Trouble getting family members names: " + e.getMessage());
        }

        return res;
      }
      public ResultSet getProteinFamilies() {
        // process data with direct SQL queries on the source database
        ResultSet res = null;
        try {
          Statement stmt = connection.createStatement();
          String query = "SELECT "
              + " grp.name AS clusterName,"
              + " grp.uniquename AS clusterId,"
              + " d.accession AS methodId, "
              + " d.description AS methodName, "
              + "(SELECT value from grpprop p where p.grp_id=grp.grp_id and p.type_id="+ChadoHmmTypeId+") AS hmm "
              + " FROM"
              + " grp, dbxref d "
              + " WHERE "
              + " db_id="+PACMethodDbId+" AND" 
              + " grp.type_id="+ChadoFamilyTypeId+" AND grp.dbxref_id=d.dbxref_id AND"
              + " accession IN ("+quotedList(methodIds)+")";
          LOG.info("Executing query: "+query);
          res = stmt.executeQuery(query);
        } catch (SQLException e) {
          throw new BuildException("Trouble method names: " + e.getMessage());
        }
        return res;
      }

      void registerCrossReferences(Integer clusterId, ProteinFamily family) throws Exception {
        /* TODO
        ResultSet res = getFamilyCrossReferences(clusterId);

        //TODO: right now we only have KOG terms to worry about.
        // This may be more complex later.
        while( res.next()) {
          String value = res.getString("value");
          String dbName = res.getString("name");
          if (!dataSourceHash.containsKey(dbName)) {
            DataSource source = getDirectDataLoader().createObject(DataSource.class);
            source.setName(dbName);
            getDirectDataLoader().store(source);
            dataSourceHash.put(dbName,source);
          }
          if (!crossRefHash.containsKey(value)) {
            CrossReference crossref = getDirectDataLoader().createObject(CrossReference.class);
            crossref.setIdentifier(value);
            crossref.setSource(dataSourceHash.get(dbName));
            getDirectDataLoader().store(crossref);
            crossRefHash.put(value,crossref);
          }
          // TODO figure out family.addToCollection("crossReferences",crossRefProxy.get(value));
        }
        res.close();
        */
      }

      protected ResultSet getFamilyCrossReferences(Integer clusterId) {
        ResultSet res;
        try {
          Statement stmt = connection.createStatement();
          String query = "select value,'KOG' as name"
              + " from"
              + " annotation a, annotationField f, objectType t"
              + " where ObjectId="+clusterId 
              + " and fieldId=f.id and a.active=1"
              + " and objectTypeId=t.id and t.name ='cluster'"
              + " and f.name='clusterKogLetter'";
          res = stmt.executeQuery(query);
        } catch (SQLException e) {
          throw new BuildException("Trouble getting cluster annotations: " + e.getMessage());
        }

        return res;
      }
      
      /* 
       * Do what we need to do to make the MSA presentable. This is really
       * a stub in case we need to do something.
       */
      String reformatMSA(String msa) {
        return msa;
      }

      /*
       * do what we have to do to reformat a hmm into something suitable for intermine
       * pretty much replacing the newline character with the 2 characters \ and n
       */
      
      String reformatHMM(String hmm) {

        if (hmm == null) return null;
        StringBuffer returnHMM = new StringBuffer();
        for( String line: hmm.split("\\n") ) {
          if (returnHMM.length() > 0) returnHMM.append("\\n");
          returnHMM.append(line);
        }
        return returnHMM.toString();
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
      
      /* to guard against duplications, we'll make a query to see which (if any)
       * clusters for this method have already been entered.
       */
      private void preFillClusters() {
        
        if (methodIds == null ) {
          return;
        }
        for(String method : methodIds.split(",")) {
          Query q = new Query();
          QueryClass qC = new QueryClass(ProteinFamily.class);
          q.addFrom(qC);
          QueryField qFId = new QueryField(qC,"clusterId");
          q.addToSelect(qFId);

          ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

          QueryField qMethodRef = new QueryField(qC, "methodId");
          Integer methodId;
          try {
            methodId = Integer.parseInt(method.replaceAll("'",""));
          } catch (NumberFormatException e) {
            throw new BuildException("method is not a simple integer");

          }
          QueryValue qMethodValue = new QueryValue(methodId);
          cs.addConstraint(new SimpleConstraint(qMethodRef,ConstraintOp.EQUALS,qMethodValue));
          q.setConstraint(cs);

          LOG.info("Prefilling Known families. Query is "+q);
          try {
            Results res = getIntegrationWriter().getObjectStore().execute(q,1000,false,false,false);
            Iterator<Object> resIter = res.iterator();
            LOG.info("Iterating...");
            while (resIter.hasNext()) {
              @SuppressWarnings("unchecked")
              ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
              Integer clusterId = (Integer)rr.get(0);
              alreadyGot.add(clusterId);
            }
          } catch (Exception e) {
            throw new BuildException("Problem in prefilling clusterid ProxyReferences: " + e.getMessage());
          }
        }
        LOG.info("Retrieved "+alreadyGot.size()+" ClusterIds.");
      }
      
      /*
       * prefill a hash of either genes or proteins.
       */
      private void preFill(Map<String,InterMineObject> map,Class<? extends InterMineObject> objectClass) {
        Query q = new Query();
        QueryClass qC = new QueryClass(objectClass);
        q.addFrom(qC);
        QueryField qFName = new QueryField(qC,"secondaryIdentifier");
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
            //((IntegrationWriterDataTrackingImpl)getIntegrationWriter()).markAsStored(id);
          }
        } catch (Exception e) {
          throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
        }
        LOG.info("Retrieved "+map.size()+" ProxyReferences.");

      }
      
      /* 
       * fill a hash of known organisms. The hash is Organism objects keyed by proteome id
       */
      
      private void preFillOrganism() {
        Query q = new Query();
        QueryClass qC = new QueryClass(Organism.class);
        q.addFrom(qC);
        q.addToSelect(qC);

        LOG.info("Prefilling Organism Hash. Query is "+q);
        try {
          Results res = getIntegrationWriter().getObjectStore().execute(q,1000,false,false,false);
          Iterator<Object> resIter = res.iterator();
          LOG.info("Iterating...");
          while (resIter.hasNext()) {
            @SuppressWarnings("unchecked")
            ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
            Organism o = (Organism)rr.get(0);
            organismHash.put(o.getProteomeId(),o);
          }
        } catch (Exception e) {
          throw new BuildException("Problem in prefilling Organism ProxyReferences: " + e.getMessage());
        }
        LOG.info("Retrieved "+organismHash.size()+" Organism ProxyReferences.");
      }
      
      /*
       * retrieve an organism from the hash. If it does not exist, register it.
       */

      private Organism getOrganism(Integer proteomeId) throws ObjectStoreException {
        Organism org = organismHash.get(proteomeId);
        if (org == null) {
          org = getDirectDataLoader().createObject(Organism.class);
          org.setProteomeId(proteomeId);
          getDirectDataLoader().store(org);
          organismHash.put(proteomeId,org);
        }
        return org;
      }
}
