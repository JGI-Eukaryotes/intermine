package org.intermine.bio.dataconversion;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
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
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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
public class PhytozomeClustersConverter extends DBDirectDataLoaderTask
{
  private static final Logger LOG = Logger.getLogger(PhytozomeClustersConverter.class);
  private Connection connection;

  protected Map<Integer,ArrayList<Integer>> methodToClusterList = new HashMap<Integer,ArrayList<Integer>>();
  protected Map<String,String> methodNames = new HashMap<String,String>();
  protected String methodIds = null;

  private Map<String,InterMineObject> geneMap = new HashMap<String, InterMineObject>();
  private Map<String,InterMineObject> protMap = new HashMap<String, InterMineObject>();
  private Map<Integer,Organism> organismHash = new HashMap<Integer,Organism>();
  private Map<String,CrossReference> crossRefHash = new HashMap<String,CrossReference>();
  private Map<String,DataSource> dataSourceHash = new HashMap<String,DataSource>();
  private Set<Integer> alreadyGot = new HashSet<Integer>();

  
  private void preFillExisting() {
    
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
        methodId = Integer.parseInt(method);
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
  /**
   * 
   *
   * {@inheritDoc}
   * @throws SQLException 
   */
  public void process() {
    
    connection = getConnection();

    preFill(geneMap,Gene.class);
    preFill(protMap,Protein.class);
    preFillOrganism();
    preFillMethodMap();
    preFillExisting();
    
    ResultSet res = getProteinFamilies();

    int ctr = 0;
    try {
      while (res.next()) {

        Integer clusterId = res.getInt("clusterId");
        if (clusterId != null ) { //&& !alreadyGot.contains(clusterId) ) {
        // uncompress and process msa and hmm
        String msaString = expandCompressedBlob(res.getBlob("zMSA"));
        String hmmString = expandCompressedBlob(res.getBlob("zHMM"));

        ProteinFamily proFamily;
        try {
          proFamily = getDirectDataLoader().createObject(ProteinFamily.class);

        } catch (ObjectStoreException e1) {
          throw new BuildException("Trouble creating protein family: "+e1.getMessage());
        }
        String clusterName = res.getString("clusterName");
        if (clusterName != null && !clusterName.trim().isEmpty() )
                                proFamily.setClusterName(clusterName.trim());
        if (clusterId != null ) proFamily.setClusterId(clusterId);
        Integer methodId = res.getInt("methodId");
        if (methodId != null ) proFamily.setMethodId(methodId);
        if (methodId != null && methodNames.containsKey(methodId) )
                             proFamily.setMethodName(methodNames.get(methodId));
        String consensusSequence = res.getString("sequence");


        HashMap<String, String> idToName;

        // this is a list of family members we will store. Separated by proteomeId.
        HashMap<Integer,ArrayList<ProteinFamilyMember>> thingsToStore = 
                       new HashMap<Integer, ArrayList<ProteinFamilyMember>>();
        
        try {
          idToName = registerProteins(clusterId,proFamily,thingsToStore);
        } catch (Exception e1) {
          throw new BuildException("Problem trying to register proteins: "+e1.getMessage());
        }

        // for singletons, there will (never?) be a consensus sequence
        // it is the protein sequence.
        if ((idToName.keySet().size()==1) && (consensusSequence==null) ) {
          consensusSequence = getPeptideSequence((String)idToName.keySet().toArray()[0]);
        }
        if (consensusSequence != null && !consensusSequence.isEmpty()) {
          Sequence sequence;
          try {
            sequence = storeSequence(consensusSequence);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem trying to register proteins: "+e.getMessage());
          }
          proFamily.setConsensus(sequence);
        }

        if (msaString != null) {
          String newMSA = reformatMSA(msaString,idToName);
          String newHMM = reformatHMM(hmmString);
          MSA msa;
          try {
            msa = getDirectDataLoader().createObject(MSA.class);
          } catch (ObjectStoreException e1) {
            throw new BuildException("Trouble when creating MSA: "+e1.getMessage());
          }
          msa.setPrimaryIdentifier("Cluster "+clusterId+" alignment");
          msa.setAlignment(newMSA);
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
          throw new BuildException("Problem storing protein family." + e.getMessage());
        }
      }
      }
    } catch (SQLException e) {
      throw new BuildException("There was an SQL exception: "+e.getMessage());
    }
 
    LOG.info("Stored "+ctr+" clusters.");
  }

  HashMap<String,String> registerProteins(Integer clusterId, ProteinFamily family,
      HashMap<Integer,ArrayList<ProteinFamilyMember>> thingsToStore) throws Exception{
    ResultSet res = getFamilyMembers(clusterId);
    
    // we need this id->name to reformat the MSA
    HashMap<String,String> idToName = new HashMap<String,String>();
    
    
    
    while( res.next()) {
      Integer proteomeId = res.getInt("proteomeid");
      idToName.put(res.getString("transcriptId"), res.getString("peptideName"));

      String pacID = "PAC:"+res.getString("transcriptId");
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
      pfm.setMembershipDetail(res.getString("name"));
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
    return idToName;
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
      String query = "SELECT peptide FROM "
          + " transcript"
          + " WHERE id="+transcriptId;
      res = stmt.executeQuery(query);
      // should only have 1 row; we'll return the first non-empty value,
      // but we'll remove terminal stops.
      while( res.next()) {
        String peptide = res.getString("peptide");
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

  public ResultSet getFamilyMembers(Integer clusterId) {
    ResultSet res = null;
    try {
      Statement stmt = connection.createStatement();
      String query = "select peptideName,"
          + " locusName, "
          + " t.id as transcriptId, "
          + " t.proteomeId as proteomeid, "
          + " m.name from"
          + " clusterJoin c, transcript t, membershipDetail m"
          + " where clusterId="+clusterId 
          + " and memberId=t.id and c.active=1"
          + " and m.id=memShipDetailId"
          + " and t.active=1 and t.proteomeId=c.proteomeId";
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
      //String query = "SELECT zMSA,zHMM,sequence,clusterName,"
      String query = "SELECT null as zMSA,null as zHMM,sequence,clusterName,"
          + " clusterDetail.id as clusterId,"
          + " methodId AS methodId"
          + " FROM"
          + " clusterDetail LEFT OUTER JOIN"
          + " (msa LEFT OUTER JOIN centroid"
          + " ON centroid.msaId=msa.id"
          + " LEFT OUTER JOIN hmm on hmm.msaId=msa.id) "
          + " ON msa.clusterId=clusterDetail.id"
          + " WHERE"
          + " clusterDetail.active=1 AND"
          + " clusterDetail.methodId IN ("+methodIds+")";
      LOG.info("Executing query: "+query);
      res = stmt.executeQuery(query);
    } catch (SQLException e) {
      throw new BuildException("Trouble method names: " + e.getMessage());
    }
    return res;
  }

  void registerCrossReferences(Integer clusterId, ProteinFamily family) throws Exception {
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
  String reformatMSA(String msa, HashMap<String,String>idToName) {
    // idLength is the max length of the id string, nameLength is the
    // max length of the replacement id string. The difference is the
    // number of spaces we need to pad
    //
    // The transcript numbers could be of different width, but I'm 
    // assuming one of them has no space before the start.
    int nameLength = 0;
    int idLength = 0;
    for (String id : idToName.keySet() ) {
      idLength = (id.length() > idLength)?
          id.length():idLength;
          nameLength = (idToName.get(id).length() >nameLength)?
              idToName.get(id).length():nameLength;
    }
    String[] lines = msa.split("\\n");
    String[] processedLines = new String[lines.length];
    // precompile patterns
    HashMap<String,Pattern> idToPattern = new HashMap<String,Pattern>();
    for ( String id : idToName.keySet()) {
      idToPattern.put(id, Pattern.compile(" *"+id+" .*"));
    }

    int lineCtr = 0;
    for( String line : lines ) {
      // scan through every line, replacing id with name:id
      for( String id : idToName.keySet() ) {
        // Look for maybe space at the beginning,
        // the number and a single space character
        // followed by the rest of the line
        Pattern p = idToPattern.get(id);
        if (p.matcher(line).matches()) {
          String replacement = idToName.get(id)+":"+id;
          int nSpaces = nameLength - idToName.get(id).length() + 1;
          StringBuffer s = new StringBuffer(line.replaceFirst(" *"+id+" ", replacement));
          for(int i=0;i<nSpaces;i++) s.insert(0, " ");
          processedLines[lineCtr] = s.toString();
          break;
        }
      }
      // not touched? copy. maybe with spaces.
      if (processedLines[lineCtr] == null) {
        if (line.length() == 0) {
          processedLines[lineCtr] = line;
        } else if (lineCtr == 1) {
          // header line. Do not pad with spaces
          processedLines[lineCtr] = line;
        } else {
          // add spaces
          StringBuffer s = new StringBuffer();
          // we're also adding a :. So +1
          for(int i=0;i<nameLength+1;i++) s.append(" ");
          s.append(line);
          processedLines[lineCtr] =s.toString();
        }
      }
      lineCtr++;
    }
    StringBuffer returnMSA = new StringBuffer();
    for( String line: processedLines ) {
      if (returnMSA.length() > 0) returnMSA.append("\\n");
      returnMSA.append(line);
    }
    return returnMSA.toString();
  }
  String reformatHMM(String hmm) {

    //do what we have to do to reformat a hmm into something suitable for intermine
    // pretty much replacing the newline character with the 2 characters \ and n

    if (hmm == null) return null;
    StringBuffer returnHMM = new StringBuffer();
    for( String line: hmm.split("\\n") ) {
      if (returnHMM.length() > 0) returnHMM.append("\\n");
      returnHMM.append(line);
    }
    return returnHMM.toString();
  }
  String expandCompressedBlob(Blob z) { 
    if ( z == null) {
      return null;
    }
    Inflater inf = new Inflater();
    try {
      inf.setInput(z.getBytes(1,(int) z.length()));
    } catch (SQLException e) {
      throw new BuildException("Problem getting bytes from compressed blob." + e.getMessage());
    }
    int increment;
    StringBuffer uncompressedStringBuffer = new StringBuffer();
    byte[] byteBuffer = new byte[1000];
    do {
      try {
        increment = inf.inflate(byteBuffer,0,1000);
      } catch (DataFormatException e) {
        throw new BuildException("Problem inflating blob." + e.getMessage());
      }
      try {
        uncompressedStringBuffer.append(new String(byteBuffer,0,increment,"UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new BuildException("Problem encoding blob." + e.getMessage());
      }
    } while (inf.getRemaining() > 0);
    return uncompressedStringBuffer.toString();
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
  
  private void preFillMethodMap()
  {
    if (methodIds == null) return;
    String query = "select id,name,adjective from method where id in ("+methodIds+")";
    try {
      Statement stmt = connection.createStatement();
      ResultSet res = stmt.executeQuery(query);
      while (res.next() ) {
        String name = res.getString("name");
        if (name==null) {
          name = res.getString("adjective");
        }
        methodNames.put((new Integer(res.getInt("id"))).toString(),name);
      }
      res.close();
    } catch (SQLException e) {
      throw new BuildException("Trouble method names: " + e.getMessage());
    }
  }

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