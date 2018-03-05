package org.intermine.bio.dataconversion;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.bio.dataconversion.ChadoDbDirectConfig;
import org.intermine.bio.util.OrganismData;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.*;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * @author jcarlson
 *
 */
public class ChadoDbDirectProcessor {

  // PRIVATE CLASS VARIABLES

  protected static final Logger LOG =
      Logger.getLogger(ChadoDbDirectProcessor.class);
  
  // the name of the temporary tables we create from the feature table
  // to speed up processing
  protected String tempChromosomeTableName = "intermine_chado_chromosomes";
  protected String tempFeatureTableName = "intermine_chado_features";

  // PRIVATE HASHMAPS
  // these keep track of the interproscan db's and hits in the db's.
  protected Map<String,InterMineObject> dataSourceMap;
  protected Map<String,HashMap<String,InterMineObject>> crossReferenceMap;
  // and we're also going to make an entry in the ontology term table.
  protected Map<String,InterMineObject> ontologyMap;
  protected Map<String,InterMineObject> goTermMap;
  protected Map<String,HashMap<String,InterMineObject>> ontologyTermMap;
  // and entered protein domains
  protected Map<String,InterMineObject> proteindomainMap;
  // a map from chado feature_id to it's stored InterMineObject
  protected Map<Integer,InterMineObject> storedChadoObjects;
  
  ChadoDbDirectConverter converter;  // the caller
  Organism organism; // the intermine organism
  Integer organismId; // the chado organism_id
  Integer assemblyDbxrefId;
  Integer annotationDbxrefId;
  ChadoDbDirectConfig config;
  
  public ChadoDbDirectProcessor(ChadoDbDirectConverter conv,
      Organism o,Integer oId, Integer assembly,Integer annotation) {
    converter = conv;
    organism = o;
    organismId = oId;
    assemblyDbxrefId = assembly;
    annotationDbxrefId = annotation;
    config = new ChadoDbDirectConfig(converter.getConnection());
    dataSourceMap = new HashMap<String,InterMineObject>();
    crossReferenceMap = new HashMap<String,HashMap<String,InterMineObject>>();
    ontologyMap = new HashMap<String,InterMineObject>();
    goTermMap = new HashMap<String,InterMineObject>();
    ontologyTermMap = new HashMap<String,HashMap<String,InterMineObject>>();
    proteindomainMap = new HashMap<String,InterMineObject>();
    prefillSimpleMap(dataSourceMap,DataSource.class,"name");
    prefillSimpleMap(ontologyMap,Ontology.class,"name");
    prefillSimpleMap(goTermMap,GOTerm.class,"identifier");
    prefillDoubleMap(ontologyTermMap,Ontology.class,"name",OntologyTerm.class,"identifier");
    prefillDoubleMap(crossReferenceMap,DataSource.class,"name",CrossReference.class,"identifier");
    prefillSimpleMap(proteindomainMap,ProteinDomain.class,"primaryIdentifier");
    storedChadoObjects = new HashMap<Integer,InterMineObject>();
  }
  
  public void process() throws SQLException, ObjectStoreException {
    // the steps
    System.out.println("Chromosomes...");
    fillChromosomeTable();
    System.out.println("Annotations...");
    try {
      fillAnnotationTable();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      throw new BuildException("Class was not found when filling annotation table.");
    }
    System.out.println("Properties...");
    fillProperties();
    System.out.println("Synonyms...");
    fillSynonyms();
    System.out.println("Relationships...");
    fillRelationships();
    System.out.println("Analyses...");
    fillAnalyses();
    System.out.println("Gene Annotations...");
    fillGeneAnnotations();
    System.out.println("Done.");
    System.out.println("Storing...");
    for( InterMineObject i: storedChadoObjects.values()) {
      converter.getDirectDataLoader().store(i);
    }
 
    Connection conn = converter.getConnection();
    conn.createStatement().execute(
        "DROP TABLE "+ tempChromosomeTableName);
    conn.createStatement().execute(
        "DROP TABLE "+ tempFeatureTableName);
    conn.close();
  }

  private void fillChromosomeTable() throws ObjectStoreException, SQLException {
    String chromosomeTypeIdsString =
        ChadoDbDirectConfig.getSQLString(config.getFeatures("chromosome"));
    String genomeTypeIdsString =
        ChadoDbDirectConfig.getSQLString(config.getFeatures("genome"));
    String relationTypeIdsString =
        ChadoDbDirectConfig.getSQLString(config.getRelations("contained"));

    String verifyGenomeQuery =
        "SELECT count(feature_id) FROM feature g WHERE "
            + "  g.type_id " + genomeTypeIdsString + " AND "
            + "  organism_id = " + organismId + " AND "
            + " dbxref_id = " + assemblyDbxrefId;
    Statement verifyStatement = converter.getConnection().createStatement();
    try {
      ResultSet res = verifyStatement.executeQuery(verifyGenomeQuery);
      while (res.next()) {
        Integer count = new Integer(res.getInt("count"));
        if (count > 1) {
          throw new BuildException("There are " + count + " genomes that satisfy dbxref_id/is_obsolete condition. " );
        } else if (count == 0) {
          throw new BuildException("There are no genomes that satisfy dbxref_id/is_obsolete condition. " );
        }
      }
      res.close();
    } catch (SQLException e) {
      LOG.error("Problem counting the genomes.");
      throw new BuildException("Problem when counting the genomes " + e);
    }
                                             
    String query =
        "CREATE TABLE " + tempChromosomeTableName + " AS"
            + " SELECT c.feature_id, c.name, c.uniquename,"
            + " c.seqlen, c.residues,"
            + " md5(c.residues) as md5checksum, c.organism_id"
            + " FROM "
            + " feature c, feature g, feature_relationship r"
            + " WHERE c.type_id " + chromosomeTypeIdsString
            + "  AND g.type_id " + genomeTypeIdsString
            + "  AND r.type_id " + relationTypeIdsString
            + "  AND r.object_id=g.feature_id"
            + "  AND r.subject_id=c.feature_id"
            + "  AND c.is_obsolete='f'"
            + "  AND c.dbxref_id="+ assemblyDbxrefId 
            + "  AND g.dbxref_id="+ assemblyDbxrefId
            + "  AND g.organism_id="+organismId
            + "  AND c.organism_id="+organismId;         

    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    LOG.info("executing createChromosomeTempTable(): " + query);
    try {
      stmt.execute(query);
      LOG.info("Done with query.");
      String idIndexQuery = "CREATE INDEX " + tempChromosomeTableName +
          "_feature_index ON " + tempChromosomeTableName + "(feature_id)";
      LOG.info("Executing: " + idIndexQuery);
      stmt.execute(idIndexQuery);
      String analyze = "ANALYZE " + tempChromosomeTableName;
      LOG.info("Executing: " + analyze);
      stmt.execute(analyze);
      LOG.info("Done with analyze.");
      LOG.info("Querying temp chromosome table.");
    } catch (SQLException e) {
      throw new BuildException("Trouble making chromosome table: "+
          e.getMessage());
    }
    String selectQuery = "SELECT * FROM " + tempChromosomeTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String residues = res.getString("residues");
      String checksum = res.getString("md5checksum");
      int seqlen = 0;
      if (residues != null) {
        seqlen = residues.length();
      }
      Integer seqId = null;
      Sequence seq = null;
      if(seqlen > 0 && residues != null ) {
        seq = converter.getDirectDataLoader().createObject(Sequence.class);
        seq.setResidues(new PendingClob(residues));
        seq.setLength(seqlen);
        seq.setMd5checksum(checksum);
        seqId = seq.getId();
        converter.getDirectDataLoader().store(seq);
      }
      Chromosome chrom = converter.getDirectDataLoader().createObject(Chromosome.class);
      chrom.setPrimaryIdentifier(name);
      chrom.setName(name);
      chrom.setSecondaryIdentifier(uniqueName);
      chrom.setLength(seqlen);
      chrom.setOrganism(organism);
      if (seq != null) chrom.setSequence(seq);
      //converter.getDirectDataLoader().store(chrom);
      storedChadoObjects.put(featureId,chrom);
      count++;
    }
    LOG.info("Created " + count + " chromosomes");
    res.close();
    stmt.close();
    
  }

  private void fillAnnotationTable() throws ObjectStoreException, SQLException, ClassNotFoundException {

    String query =
        "CREATE TABLE " + tempFeatureTableName + " AS " +
            "SELECT f.feature_id, f.name, f.uniquename," +
            "f.type_id,f.seqlen," +
            "f.residues, md5(f.residues) as md5checksum," +
            "l.featureloc_id, l.srcfeature_id, l.fmin," +
            "l.is_fmin_partial, l.fmax, l.is_fmax_partial," +
            "l.strand " +
            "FROM feature f, " + 
            tempChromosomeTableName + " g, " +
            "featureloc l " +
            "WHERE f.type_id IN (" + 
            config.listString("sequence",converter.getFeatureTypes()) +
            ") " +
            "AND NOT f.is_obsolete " +
            "AND f.dbxref_id = " + annotationDbxrefId +
            "AND is_analysis = 'f' " +
            "AND l.srcfeature_id = g.feature_id " +
            "AND f.feature_id = l.feature_id " +
            "AND f.organism_id =" + organismId;
    
    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    LOG.info("Creating Feature temp table(): " + query);
    try {
      stmt.execute(query);
      LOG.info("Done with feature query.");
      String idIndexQuery = "CREATE INDEX " + tempFeatureTableName +
          "_feature_index ON " + tempFeatureTableName + "(feature_id)";
      LOG.info("executing: " + idIndexQuery);
      stmt.execute(idIndexQuery);
      String analyze = "ANALYZE " + tempFeatureTableName;
      LOG.info("executing: " + analyze);
      stmt.execute(analyze);
      LOG.info("Done with analyze.");
      LOG.info("Querying temp feature table.");
    } catch (SQLException e) {
      throw new BuildException("Trouble making annotation table: "+
          e.getMessage());
    }
    
    String selectQuery = "SELECT * FROM " + tempFeatureTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String chadoType = config.cvTermInv(res.getInt("type_id"));
      String checksum = res.getString("md5checksum");
      String residues = res.getString("residues");
      Integer seqlen = res.getInt("seqlen");
      Integer srcFeatureId = res.getInt("srcfeature_id");
      Integer fmin = res.getInt("fmin");
      Integer fmax = res.getInt("fmax");
      Integer strand = res.getInt("strand");
      Boolean fminPartial = res.getBoolean("is_fmin_partial");
      Boolean fmaxPartial = res.getBoolean("is_fmax_partial");
      
      Sequence seq = null;
      // override db value of sequence length if residues is set.
      if (residues != null && !residues.isEmpty()) {
        seqlen = residues.length();
        // subtract 1 for stop codons on proteins
        if (residues.endsWith("*") ) seqlen--;
        // and store
        seq = converter.getDirectDataLoader().createObject(Sequence.class);
        seq.setResidues(new PendingClob(residues));
        seq.setLength(seqlen);
        seq.setMd5checksum(checksum);
        converter.getDirectDataLoader().store(seq);
      }
      
      InterMineObject feat = null;
      feat = ChadoDbDirectConfig.getIntermineType(chadoType).cast(converter.getDirectDataLoader().createObject(ChadoDbDirectConfig.getIntermineType(chadoType)));
      feat.setFieldValue("primaryIdentifier",name);
      feat.setFieldValue("name",name);
      feat.setFieldValue("secondaryIdentifier",uniqueName);
      feat.setFieldValue("length",seqlen);
      feat.setFieldValue("organism",organism);
      if (seq != null) feat.setFieldValue("sequence",seq);
      
      // if there is a chromosome field, set it.
      try {
        feat.getFieldType("chromosome");
        feat.setFieldValue("chromosome",(Chromosome)storedChadoObjects.get(srcFeatureId));
      } catch (IllegalArgumentException e) {}
      // see if we can set a location. Some features do not have one
      try {
        feat.getFieldType("chromosomeLocation");
        Location location = converter.getDirectDataLoader().createObject(Location.class);
        location.setStart(fmin+1);
        location.setEnd(fmax);
        location.setStrand(strand.toString());
        location.setLocatedOn((Chromosome)storedChadoObjects.get(srcFeatureId));
        location.setFeature((BioEntity) feat);
        converter.getDirectDataLoader().store(location);
        feat.setFieldValue("chromosomeLocation",location);
      } catch (IllegalArgumentException e) {}

      storedChadoObjects.put(featureId,feat);
      
      count++;
    }
    LOG.info("created " + count + " features");
    res.close();
    stmt.close();
    
  }
  
  private void fillAnalyses() throws SQLException, ObjectStoreException {
    
    // first, find the important analysis records.   
    StringBuffer analysisIds = new StringBuffer();
    
    String query = "SELECT analysis_id FROM analysis a, feature f"
        + " WHERE"
        + " a.program in (" + ChadoDbDirectConfig.ANALYSIS_PROGRAMS + ")"
        + " AND a.sourcename::int = f.feature_id "
        + " AND f.dbxref_id = " + annotationDbxrefId 
        + " AND f.type_id="+config.cvTerm("sequence","peptide_collection");
    LOG.info("executing analysis_id query: " + query);
    ResultSet aRes = null;
    try {
      Connection conn = converter.getConnection();
      Statement stmt = conn.createStatement();
      aRes = stmt.executeQuery(query);
    } catch (SQLException e) {
      throw new BuildException("Problem when querying analyses " + e);
    }
    LOG.info("got resultset.");
    int counter = 0;
    try {
      while (aRes.next()) {
        if (analysisIds.length() > 0) {
          analysisIds.append(",");
        }
        analysisIds.append(new Integer(aRes.getInt(1)));
      }
    } catch (SQLException e) {
      throw new BuildException("Problem when querying CVTerms " + e);
    }
    aRes.close();
    
    String pQuery = "SELECT p.feature_id as protein_analysis_feature_id," +
        "p.name as match_name, " +
        "f.name as protein_name," +
        "f.uniquename as protein_uniquename," +
        "db.name as db_name," +
        "dbx.accession," +
        "f.feature_id as protein_feature_id," +
        "l.fmin, l.fmax," +
        "afp.value as program_name," +
        "af.rawscore, af.normscore, af.significance," +
        "af.identity " +
        "FROM " + tempFeatureTableName + " f," +
        "feature m, feature p," +
        "feature_relationship fm, feature_relationship mp," +
        "analysisfeature af, analysisfeatureprop afp," +
        "featureloc l, dbxref dbx, db "+
        "WHERE l.srcfeature_id=f.feature_id " +
        "AND l.feature_id=p.feature_id " +
        "AND f.type_id = " + config.cvTerm("sequence","polypeptide") + " " +
        "AND m.type_id = " + config.cvTerm("sequence","match") + " " +
        "AND p.type_id = " + config.cvTerm("sequence","match_part") + " " +
        "AND fm.type_id = " + config.cvTerm("relationship","contained_in") + " " +
        "AND mp.type_id = " + config.cvTerm("relationship","part_of") + " " +
        "AND afp.type_id = " + config.cvTerm("sequence","evidence_for_feature") + " " +
        "AND m.is_obsolete='f' " +
        "AND p.is_obsolete='f' " +
        "AND afp.rank=0 " +
        "AND m.organism_id=" + organismId + " " +
        "AND p.organism_id=" + organismId + " " +
        "AND m.organism_id=p.organism_id " +
        "AND fm.object_id=f.feature_id " +
        "AND fm.subject_id=m.feature_id " +
        "AND mp.object_id=m.feature_id " +
        "AND mp.subject_id=p.feature_id " +
        "AND p.feature_id=af.feature_id " +
        "AND af.analysisfeature_id = afp.analysisfeature_id " +
        "AND p.dbxref_id=dbx.dbxref_id " +
        "AND af.analysis_id IN ("+analysisIds.toString()+")" +
        "AND dbx.db_id=db.db_id";

    LOG.info("executing getProteinFeatureResultSet(): " + pQuery);
    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet res = stmt.executeQuery(pQuery);
    LOG.info("Got resultset.");

    int count=0;
    while(res.next()) {
      Integer proteinAnalysisFeatureId = res.getInt("protein_analysis_feature_id");
      Integer proteinFeatureId = res.getInt("protein_feature_id");
      String dbName = res.getString("db_name");
      // what the feature hits
      String proteinName = res.getString("protein_name");
      String proteinUniquename = res.getString("protein_uniquename");
      // the primary identifier we give to the feature
      String hitName = res.getString("match_name");
      // the thing the feature hits
      String accession = res.getString("accession");
      // the program that made the assignment
      String programName = res.getString("program_name");
      // min and max. convert to 1-indexed. SIGNALP is
      // a special case since it is a cleavage site.
      Integer fmin = "SIGNALP".equals(dbName)?
          res.getInt("fmin") - 1:
            res.getInt("fmin") + 1;
      Integer fmax = res.getInt("fmax");
      // assuming strand = 1
      Double rawScore = res.getDouble("rawscore");
      if (res.wasNull()) rawScore = null;
      Double normScore = res.getDouble("normscore");
      if (res.wasNull()) normScore = null;
      Double significance = res.getDouble("significance");
      if (res.wasNull()) significance = null;
      
      if( processAndStoreProteinAnalysisFeature(proteinAnalysisFeatureId,proteinFeatureId,
              hitName,proteinName,proteinUniquename,accession,dbName,programName,
              fmin,fmax,rawScore,normScore,significance) ) {
            count++;
          }
    }
    LOG.info("processed " + count + " ProteinAnalysisFeature records");
    res.close();
    stmt.close();
  }
  
  private boolean processAndStoreProteinAnalysisFeature(Integer matchId,
      Integer proteinId,String hitName, String proteinName, String proteinUniquename,
      String accession, String dbName,String programName,
      Integer fmin, Integer fmax,
      Double rawScore,Double normScore, Double significance)
          throws ObjectStoreException {

    if (!storedChadoObjects.containsKey(proteinId) ) {
      return false;
    }
    
    ProteinAnalysisFeature feature = converter.getDirectDataLoader().createObject(ProteinAnalysisFeature.class);
    feature.setOrganism(organism);
    feature.setPrimaryIdentifier(hitName);
    feature.setSecondaryIdentifier(proteinUniquename+":"+dbName);
    feature.setProtein((Protein)storedChadoObjects.get(proteinId));

    if (!dataSourceMap.containsKey(dbName)) {
      DataSource dS = converter.getDirectDataLoader().createObject(DataSource.class);
      dS.setName(dbName);
      converter.getDirectDataLoader().store(dS);
      crossReferenceMap.put(dbName,new HashMap<String,InterMineObject>());
      dataSourceMap.put(dbName,dS);
      // this also means we need a ontology
      Ontology oN = converter.getDirectDataLoader().createObject(Ontology.class);
      oN.setName(dbName);
      converter.getDirectDataLoader().store(oN);
      ontologyMap.put(dbName,oN);
      ontologyTermMap.put(dbName, new HashMap<String,InterMineObject> ());
    }
    DataSource dataSourceIdentifier = (DataSource)dataSourceMap.get(dbName);

    if (!crossReferenceMap.get(dbName).containsKey(accession)) {
      CrossReference newItem = converter.getDirectDataLoader().createObject(CrossReference.class);
      newItem.setIdentifier(accession);
      newItem.setSource((DataSource)dataSourceMap.get(dataSourceIdentifier));
      converter.getDirectDataLoader().store(newItem);
      crossReferenceMap.get(dbName).put(accession,newItem);
    }
    if (!ontologyTermMap.get(dbName).containsKey(accession) ) {
      // and ontology term. If desired
      OntologyTerm newTerm = converter.getDirectDataLoader().createObject(OntologyTerm.class);
      newTerm.setOntology((Ontology)ontologyMap.get(dbName));
      newTerm.setIdentifier(accession);
      converter.getDirectDataLoader().store(newTerm);
      ontologyTermMap.get(dbName).put(accession, newTerm);
    }
    feature.setCrossReference((CrossReference)crossReferenceMap.get(dbName).get(accession));

    feature.setProgramname(programName);
    if(normScore != null) {
      feature.setNormscore(normScore);
    }
    if(rawScore != null) {
      feature.setRawscore(rawScore);
    }
    if(significance != null) {
      feature.setSignificance(significance);
    }
    converter.getDirectDataLoader().store(feature);
    Location location = converter.getDirectDataLoader().createObject(Location.class);
    location.setStart(fmin);
    location.setEnd(fmax);
    location.setStrand("+1");
    location.setLocatedOn((BioEntity)storedChadoObjects.get(proteinId));
    location.setFeature(feature);
    converter.getDirectDataLoader().store(location);
    return true;
  }

  
  private void fillSynonyms() throws ObjectStoreException, SQLException {
    // and for the synonyms

    String query =
            "SELECT " +
            "f.feature_id AS feature_id," +
            "s.name as synonym " +
            "FROM " +
            tempFeatureTableName + " f," +
            "feature_synonym fs, synonym s " +
            "WHERE f.feature_id=fs.feature_id " +
            "AND fs.synonym_id=s.synonym_id " +
            "ORDER BY feature_id, synonym";
            
    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    while (res.next()) {
      Integer featureId = res.getInt("feature_id");
      String synonym = res.getString("synonym");
      Synonym syn = converter.getDirectDataLoader().createObject(Synonym.class);
      syn.setValue(synonym);
      syn.setSubject((BioEntity)storedChadoObjects.get(featureId));
      converter.getDirectDataLoader().store(syn);
      count++;
    }
    LOG.info("created " + count + " synonyms.");
    res.close();
    stmt.close();
  
  }
  
  private void fillGeneAnnotations() throws ObjectStoreException, SQLException {
    // analysis results, GO terms and IPR domains attached to genes and proteins

    Protein p;
    String query =
            "SELECT DISTINCT " +
            "f.feature_id, " +
            "f.type_id, " +
            "d.name, " +
            "x.accession " +
            "FROM " +
            tempFeatureTableName + " f," +
            "feature_dbxref fd, dbxref x, db d " +
            "WHERE f.feature_id=fd.feature_id " +
            "AND fd.dbxref_id=x.dbxref_id " +
            "AND fd.is_current " +
            "AND x.db_id=d.db_id " +
            "AND d.name NOT like 'PAC%' " +
            "ORDER BY 1,2,3";
            
    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    
    // for storing protein domains, we add these to the protein collections.
    // Storage happens after all processing
    // the key is the chado feature id.
    HashMap<Integer,ReferenceList> collections = new HashMap<Integer,ReferenceList>();
    while (res.next()) {
      Integer featureId = res.getInt("feature_id");
      Integer typeId = res.getInt("type_id");
      String dbName = res.getString("name");
      String accession = res.getString("accession");
      // this db should have been registers already!
      if (!ontologyMap.containsKey(dbName)) {
        LOG.warn("Unexpected new db for a xref: "+dbName);
        Ontology newOntology = converter.getDirectDataLoader().createObject(Ontology.class);
        newOntology.setName(dbName);
        converter.getDirectDataLoader().store(newOntology);
        ontologyMap.put(dbName,newOntology);
        ontologyTermMap.put(dbName,new HashMap<String,InterMineObject>());
      }
      // we handle InterPro (protein domains) slightly differently. In addition
      // to a ontology annotation, we add a protein domain.
      if (dbName.equals("InterPro") && ChadoDbDirectConfig.getIntermineType(config.cvTermInv(typeId)).equals("Protein")) {
        if (!proteindomainMap.containsKey(accession)) {
          ProteinDomain pD = converter.getDirectDataLoader().createObject(ProteinDomain.class);
          pD.setPrimaryIdentifier(accession);
          converter.getDirectDataLoader().store(pD);
          proteindomainMap.put(accession,pD);
        }
        ProteinDomain pD = (ProteinDomain)proteindomainMap.get(accession);
        ((Protein)storedChadoObjects.get(featureId)).addProteinDomains(pD);
        //converter.getDirectDataLoader().store(storedChadoObjects.get(featureId));
      }
      if (!ontologyTermMap.get(dbName).containsKey(accession)) {
        // and ontology term.
        LOG.warn("Unexpected new accession "+accession+" in db "+dbName);
        OntologyTerm newTerm = converter.getDirectDataLoader().createObject(OntologyTerm.class);
        newTerm.setOntology((Ontology)ontologyMap.get(dbName));
        newTerm.setIdentifier(accession);
        converter.getDirectDataLoader().store(newTerm);
        ontologyTermMap.get(dbName).put(accession, newTerm);
      }
      if (dbName.equals("GO") && ChadoDbDirectConfig.getIntermineType(config.cvTermInv(typeId)).equals("Gene")) {
        GOAnnotation oA = converter.getDirectDataLoader().createObject(GOAnnotation.class);
        oA.setSubject((BioEntity)storedChadoObjects.get(featureId));
        oA.setOntologyTerm((OntologyTerm)ontologyTermMap.get(dbName).get(accession));
        converter.getDirectDataLoader().store(oA);
       // converter.getDirectDataLoader().store(converter.objectIdentifier(featureId));
      } else {
        OntologyAnnotation oA = converter.getDirectDataLoader().createObject(OntologyAnnotation.class);
        oA.setSubject((BioEntity)storedChadoObjects.get(featureId));
        oA.setOntologyTerm((OntologyTerm)ontologyTermMap.get(dbName).get(accession));
        converter.getDirectDataLoader().store(oA);
       // converter.getDirectDataLoader().store(converter.objectIdentifier(featureId));
      }
      count++;
    }
    LOG.info("Created " + count + " OntologyAnnotations.");
    res.close();
    stmt.close();
  
  }
  
  private void fillProperties() throws ObjectStoreException, SQLException {
    // and for the relationships between features
    StringBuilder hack = new StringBuilder();
    for( String type : ChadoDbDirectConfig.getPropertyList("gene")) {
      if (hack.length() > 0 ) hack.append(",");
      hack.append(config.cvTerm("feature_property",type).toString());
    }   
    for( String type : ChadoDbDirectConfig.getPropertyList("mrna")) {
      if (hack.length() > 0 ) hack.append(",");
      hack.append(config.cvTerm("feature_property",type).toString());
    }
    String query =
            "SELECT " +
            "f.type_id as feature_type_id," +
            "f.feature_id AS feature_id," +
            "p.type_id as property_type_id, " +
            "p.value as property, " +
            "p.rank as rank " +
            "FROM " +
            tempFeatureTableName + " f," +
            "featureprop p " +
            "WHERE f.feature_id=p.feature_id " +
            "AND p.type_id IN (" + 
            hack.toString() +
            ") ORDER by feature_id, property_type_id, rank";
            
    Connection conn = converter .getConnection();
    Statement stmt = conn.createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    while (res.next()) {
      String propertyType = config.cvTermInv(res.getInt("property_type_id"));
      String featureType = config.cvTermInv(res.getInt("feature_type_id"));
      Integer featureId = res.getInt("feature_id");
      String property = res.getString("property");
      // special case: TODO handle casting.
      if(propertyType.equals("longest") ) {
        if (property.equals("1") ) {
          storedChadoObjects.get(featureId).setFieldValue(ChadoDbDirectConfig.getInterminePropertyName(propertyType),true);
        } else {
          storedChadoObjects.get(featureId).setFieldValue(ChadoDbDirectConfig.getInterminePropertyName(propertyType),false);
        }
      } else{
        storedChadoObjects.get(featureId).setFieldValue(ChadoDbDirectConfig.getInterminePropertyName(propertyType),property);
      }
      count++;
    }
    LOG.info("created " + count + " properties.");
    res.close();
    stmt.close();
  
  }
  private void fillRelationships() throws ObjectStoreException, SQLException {
    // and for the relationships between features
    // this picks out everything that connects A to B with a (subject,object)
    // in the feature_relationship table, and A to B through an intermediate I.
    // In the latter case, the relationship can be (subject) A is related to
    // (object)I and (subject) I is related to (object)B. But we also include
    // (subject) A is related to (object) I and (subject) B is related to (object I)
    // (as well as the relationship with subject and object reversed). Some things
    // such as proteins and CDSs are related to each other since both are subjects
    // to the same mRNA.
    String query =
            "SELECT " +
            "s.type_id as subject_type_id," +
            "o.type_id AS object_type_id," +
            "r.subject_id, r.object_id " +
            "FROM feature_relationship r," +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r.object_id " +
            "AND s.feature_id=r.subject_id " +
            "UNION " +
            "SELECT " +
            "s.type_id as subject_type_id, " +
            "o.type_id AS object_type_id, " +
            "r1.subject_id, r2.object_id " +
            "FROM feature_relationship r1, " +
            "feature_relationship r2, " +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " i," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r2.object_id " +
            "AND i.feature_id=r2.subject_id " +
            "AND i.feature_id=r1.object_id " +
            "AND s.feature_id=r1.subject_id " +
            "UNION " +
            "SELECT " +
            "s.type_id as subject_type_id, " +
            "o.type_id AS object_type_id, " +
            "r1.object_id, r2.object_id " +
            "FROM feature_relationship r1, " +
            "feature_relationship r2, " +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " i," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r2.object_id " +
            "AND i.feature_id=r2.subject_id " +
            "AND i.feature_id=r1.subject_id " +
            "AND s.feature_id=r1.object_id " +
            "AND s.type_id != o.type_id "+
            "UNION " +
            "SELECT " +
            "s.type_id as subject_type_id, " +
            "o.type_id AS object_type_id, " +
            "r1.subject_id, r2.subject_id " +
            "FROM feature_relationship r1, " +
            "feature_relationship r2, " +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " i," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r2.subject_id " +
            "AND i.feature_id=r2.object_id " +
            "AND i.feature_id=r1.object_id " +
            "AND s.feature_id=r1.subject_id " +
            "AND s.type_id != o.type_id";

    Connection conn = converter.getConnection();
    Statement stmt = conn.createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    // make a hash of things that go into collections. The MultiKey is 
    // objectId (number) and name of the collection.
    HashMap<MultiKey,ReferenceList> collections = 
        new HashMap<MultiKey,ReferenceList>();
    while (res.next()) {
      String objectType = config.cvTermInv(res.getInt("object_type_id"));
      String subjectType = config.cvTermInv(res.getInt("subject_type_id"));
      Integer objectId = res.getInt("object_id");
      Integer subjectId = res.getInt("subject_id");
      String intermineSubjectType = ChadoDbDirectConfig.getIntermineTypeName(subjectType);
      String intermineObjectType = ChadoDbDirectConfig.getIntermineTypeName(objectType);
      
      if( ChadoDbDirectConfig.hasReference(intermineSubjectType,intermineObjectType)) {
        String refMethod = ChadoDbDirectConfig.referenceMethod(intermineSubjectType,intermineObjectType);
        Method m;
        try {
          m = storedChadoObjects.get(subjectId).getClass().getMethod(refMethod,ChadoDbDirectConfig.getIntermineType(objectType));
          m.invoke(storedChadoObjects.get(subjectId),storedChadoObjects.get(objectId));
        } catch (Exception e) {
          throw new BuildException("Problem creating reference.");
        }
        //converter.getDirectDataLoader().store(storedChadoObjects.get(subjectId));
        LOG.debug("Linked types "+objectType+" and "+subjectType+" ids "+objectId+","+subjectId +
            " in aSubject-Object reference.");
        count++;
      }
      if( ChadoDbDirectConfig.hasReference(intermineObjectType, intermineSubjectType)) {
        String refMethod = ChadoDbDirectConfig.referenceMethod(intermineObjectType,intermineSubjectType);
        Method m;
        try {
          m = storedChadoObjects.get(objectId).getClass().getMethod(refMethod,ChadoDbDirectConfig.getIntermineType(subjectType));
          m.invoke(storedChadoObjects.get(objectId),storedChadoObjects.get(subjectId));
        } catch (Exception e) {
          throw new BuildException("Problem creating reference.");
        }
        //converter.getDirectDataLoader().store(storedChadoObjects.get(objectId));
        LOG.debug("Linked types "+subjectType+" and "+objectType+" ids "+objectId+","+subjectId +
            " in aSubject-Object reference.");
        count++;
      }

    }
    LOG.info("created " + count + " relationships");
    res.close();
    stmt.close();

  }

  public void prefillSimpleMap(Map<String,InterMineObject> map,Class<? extends InterMineObject> objectClass,String key) {

    Query q = new Query();
    QueryClass qC = new QueryClass(objectClass);
    q.addFrom(qC);
    QueryField qFName = new QueryField(qC,key);
    q.addToSelect(qFName);
    q.addToSelect(qC);

    LOG.info("Prefilling DataSourcess. Query is "+q);
    try {
      Results res = converter.getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
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
      throw new BuildException("Problem in prefilling simple map: " + e.getMessage());
    }
    LOG.info("Retrieved "+map.size()+" Object.");

  }
  public void prefillDoubleMap(Map<String,HashMap<String,InterMineObject>> map,Class<? extends InterMineObject> objectClass1,
                                            String key1,Class<? extends InterMineObject> objectClass2,String key2) {

    Query q = new Query();
    QueryClass qC1 = new QueryClass(objectClass1);
    q.addFrom(qC1);
    QueryClass qC2 = new QueryClass(objectClass2);
    q.addFrom(qC2);
    QueryField qFName1 = new QueryField(qC1,key1);
    QueryField qFName2 = new QueryField(qC2,key2);
    q.addToSelect(qFName1);
    q.addToSelect(qFName2);
    q.addToSelect(qC2);

    LOG.info("Prefilling DataSourcess. Query is "+q);
    try {
      Results res = converter.getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
      Iterator<Object> resIter = res.iterator();
      LOG.info("Iterating...");
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        String name0 = (String)rr.get(0);
        String name1 = (String)rr.get(1);
        InterMineObject obj = (InterMineObject)rr.get(2);
        if (!map.containsKey(name0) ) {
          map.put(name0,new HashMap<String,InterMineObject>());
        }
        map.get(name0).put(name1,obj);
      }
    } catch (Exception e) {
      throw new BuildException("Problem in prefilling simple map: " + e.getMessage());
    }
    LOG.info("Retrieved "+map.size()+" Object.");

  }
}
