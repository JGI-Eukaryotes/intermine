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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import org.apache.tools.ant.BuildException;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.DBDirectDataLoaderTask;
import org.intermine.xml.full.Item;
/**
 * 
 * @author
 */
public class ChadoDbDirectConverter extends DBDirectDataLoaderTask
{
  // 
  private static final String DATASET_TITLE = "Phytozome DB";
  private static final String DATA_SOURCE_NAME = "Phytozome";
  // constant values for db name and cv terms. Note embedded quotes.
  private static final String TAXONOMY_DB_NAME = "'Taxonomy'";
  private static final String PROTEOME_DB_NAME = "'PACProteome'";
  private static final String PROTEIN_COLLECTION = "'peptide_collection'";
  private static final String SCAFFOLD_COLLECTION = "'genome'";
  private static final String[] CHADO_FEATURE_TYPES =
    {"gene","mRNA","exon","intron","polypeptide","five_prime_UTR","three_prime_UTR","CDS"};

  // the different fields that can be set for this organism
  Integer taxonId = null;
  String assemblyVersion = null;
  String annotationVersion = null;
  // 'current' or 'early-release'
  String version = null;
  // the short name is the one that may be qualified.
  String shortName = null;
  String genus = null;
  String species = null;
  // full organism name
  String name = null;
  String commonName = null;
  Integer proteomeId = null;
  // a pointer to the proper CHADO assembly.
  Integer assemblyDbxrefId = null;
  Integer annotationDbxrefId = null;
  // the chado organism id
  Integer organismId = null;
  // what we've stored. Key by (chado) feature_id, value is the intermine object
  HashMap<Integer,InterMineObject> storedInterMineObject;
  // what we've stored. Key by (chado) feature_id, value is the (intermine) integer id
  //HashMap<Integer,Integer> storedId;

 /* public void recordObject(Integer feature,InterMineObject ob) {
      storedInterMineObject.put(feature, ob);
  }
  public void recordObject(Integer feature,Integer id) {
    storedId.put(feature, id);
}
  public InterMineObject objectIdentifier(Integer feature) {
    if (storedInterMineObject.containsKey(feature) ) {
      return storedInterMineObject.get(feature);
    }
    return null;
  }
  public Integer objectId(Integer feature) {
    if (storedId.containsKey(feature) ) {
      return storedId.get(feature);
    }
    return null;
  }*/
  public String[] getFeatureTypes() {
    // return a list of the things we expect to pull out of chado. In
    // the chado CV
    return CHADO_FEATURE_TYPES;
  }


  /**
   * {@inheritDoc}
   * @throws ObjectStoreException 
   */
  public void process() {
      // a database has been initialised from properties starting with db.phytozome-db

      Connection connection = getConnection();

      // unlike the standard chado-db processor, this will only process 1
      // organism at a time. The organism must be specified as either a taxonId,
      // a proteomeId or as a genus and species. Other fields may be specified.
      // Anything left blank will be filled in during post-processing
      
      try {
        fillOrganismInfo(connection);
      } catch (SQLException e1) {
        // TODO Auto-generated catch block
        throw new BuildException("SQL problem registering organism.");
      }
      // now register the organism
      Organism organismObject;
      try {
        organismObject = getDirectDataLoader().createObject(Organism.class);
      } catch (ObjectStoreException e1) {
        throw new BuildException("Problem getting an objectstore.");
      }
      // we must have specified the genome and proteome
      if (assemblyDbxrefId == null || annotationDbxrefId == null) {
        throw new BuildException("assemby or annotation dbxref_id is not set. Not proceeding.");
      }
      if (genus != null) organismObject.setGenus(genus);
      if (species != null) organismObject.setSpecies(species);
      if (genus != null && species != null) organismObject.setName(genus+" "+species);
      if (shortName != null) organismObject.setShortName(shortName);
      if (proteomeId != null) organismObject.setProteomeId(proteomeId);
      if (taxonId != null) organismObject.setTaxonId(taxonId);
      if (version != null) organismObject.setVersion(version);
      if (commonName != null) organismObject.setCommonName(commonName);
      if (assemblyVersion != null) organismObject.setAssemblyVersion(assemblyVersion);
      if (annotationVersion != null) organismObject.setAnnotationVersion(annotationVersion);
      
      try {
        getDirectDataLoader().store(organismObject);
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing organism: "+e.getMessage());
      }
      
      // the worker bee
      ChadoDbDirectProcessor p = new ChadoDbDirectProcessor(this,organismObject,organismId,assemblyDbxrefId,annotationDbxrefId);
      // release the hounds
      try {
        p.process();
        connection.close();
      } catch (SQLException e) {
        throw new BuildException("SQL problem creating features: "+e.getMessage());
      } catch (ObjectStoreException e) {
        throw new BuildException("Problem with the objectstore: "+e.getMessage());
      }
  }
  
 
  private void fillOrganismInfo(Connection conn) throws SQLException {
    // make sure enough of the organism has been specified. We'll do this
    // by making sure we can retrieve a chado organism_id
    // this is as much a test of registry in chado
    // if things are over specified (taxonId and genus and species), this
    // will run a consistency check

    Statement stmt = conn.createStatement();
    String orgQuery = "SELECT DISTINCT "+
        "o.organism_id,genus,species,accession as taxon_id FROM "+
        "organism o, organism_dbxref od, dbxref d, db "+
        "WHERE "+
        "d.db_id=db.db_id AND "+
        "db.name="+TAXONOMY_DB_NAME+" AND "+
        (taxonId!=null?
            ("d.accession='"+taxonId+"' AND "):("")) +
        (genus!=null?
            ("genus='"+genus+"' AND "):("")) +
        (species!=null?
            ("species='"+species+"' AND "):("")) +
        "d.dbxref_id = od.dbxref_id AND "+
        "o.organism_id=od.organism_id";
    ResultSet orgRes = stmt.executeQuery(orgQuery);
    while (orgRes.next()) {
      // check for duplicates
      if (organismId != null &&  !organismId.equals(orgRes.getInt("organism_id")) ) {
        throw new BuildException("Retrieved multiple organism_id values from CHADO.");
      }
      organismId = orgRes.getInt("organism_id");
      if (genus != null && !genus.equals(orgRes.getString("genus")) ) {
        throw new BuildException("Retrieved multiple genus values from CHADO.");
      }
      genus = orgRes.getString("genus");           
      if (species != null && !species.equals(orgRes.getString("species")) ) {
        throw new BuildException("Retrieved multiple species values from CHADO.");
      }
      species = orgRes.getString("species");
      if (taxonId != null && !taxonId.toString().equals(orgRes.getString("taxon_id")) ) {
        throw new BuildException("Retrieved multiple taxonId values from CHADO.");
      }
      taxonId = orgRes.getInt("taxon_id");
      organismId = orgRes.getInt("organism_id");
    }
    orgRes.close();
    if (organismId == null) {
      throw new BuildException("There is no organism associated with taxonId "+taxonId);
    }

    // now find annotation dbxref and/or proteomeId. This will
    // check for consistency if both are given, or find both if
    // neither are given (if there is only 1 for that organism)

    String annQuery = "SELECT DISTINCT "+
        "f.dbxref_id,accession as proteome_id FROM "+
        "cvterm c, cv, "+
        "feature f, feature_dbxref fd, dbxref d, db "+
        "WHERE "+
        "cv.cv_id=c.cv_id AND cv.name='sequence' AND "+
        "c.cvterm_id=f.type_id AND "+
        "c.name="+PROTEIN_COLLECTION+" AND "+
        "d.db_id=db.db_id AND "+
        "db.name="+PROTEOME_DB_NAME+" AND "+
        (proteomeId!=null?
            ("d.accession='"+proteomeId+"' AND "):("")) +
        (annotationDbxrefId!=null?
            ("f.dbxref_id='"+annotationDbxrefId+"' AND "):("")) +
        (((annotationDbxrefId==null) && (proteomeId==null))?
            ("f.is_obsolete='f' AND "):("")) +
        "f.organism_id="+organismId+" AND "+
        "d.dbxref_id = fd.dbxref_id AND "+
        "f.feature_id=fd.feature_id";
    ResultSet annRes = stmt.executeQuery(annQuery);
    int ctr = 0;
    while (annRes.next()) {
      // check for duplicates
      if (annotationDbxrefId != null &&
          !annotationDbxrefId.equals(annRes.getInt("dbxref_id")) ) {
        throw new BuildException("Retrieved annotation dbxref_id's from CHADO.");
      }
      annotationDbxrefId = annRes.getInt("dbxref_id");
      if (proteomeId != null &&
          !proteomeId.toString().equals(annRes.getString("proteome_id")) ) {
        throw new BuildException("Retrieved multiple proteome id's from CHADO.");
      }
      proteomeId = annRes.getInt("proteome_id");
      ctr++;
    }
    annRes.close();
    if (ctr == 0) {
      // if we got no records, it could be because the specified dbxref_id and protoemeId
      // were inconsistent. Or nothing is there.
      throw new BuildException("There is no annotation associated with the specification.");
    }
    
    // now the assembly dbxref_id
    String assQuery = "SELECT DISTINCT "+
        "g.dbxref_id FROM "+
        "cvterm c, cv, "+
        "feature g, feature_relationship r, feature p "+
        "WHERE "+
        "p.organism_id="+organismId+" AND "+
        "p.dbxref_id="+annotationDbxrefId+" AND "+
        "g.organism_id="+organismId+" AND "+
        "r.subject_id=p.feature_id AND "+
        "r.object_id=g.feature_id AND "+
        "g.type_id=c.cvterm_id AND " +        
        (assemblyDbxrefId!=null?
            ("g.dbxref_id='"+assemblyDbxrefId+"' AND "):
            ("g.is_obsolete='f' AND ")) +
        "cv.name='sequence' AND " +
        "c.name="+SCAFFOLD_COLLECTION;
    ctr = 0;
    ResultSet assRes = stmt.executeQuery(assQuery);
    while (assRes.next()) {
      // check for duplicates
      if (assemblyDbxrefId != null && !assemblyDbxrefId.equals(assRes.getInt("dbxref_id")) ) {
        throw new BuildException("Retrieved multiple assembly dbxref_id's.");
      }
      assemblyDbxrefId = assRes.getInt("dbxref_id");
      ctr++;
    }
    assRes.close();
    if (ctr == 0) {
      // if we got no records, it could be because the specified dbxref_id and protoemeId
      // were inconsistent. Or nothing is there.
      throw new BuildException("There is no assembly associated with the specification.");
    }
  }
  public String getDataSetTitle(int taxonId) {
      return DATASET_TITLE;
  }
  // setters for organism attributes
  public void setTaxonId(String taxonInput) {
    try {
      taxonId = new Integer(taxonInput);
    } catch (NumberFormatException e) {
      throw new BuildException("Taxon string "+taxonInput+" cannot be parsed as a integer.");
    }
  }
  public void setAssemblyVersion(String assemblyInput) {
    assemblyVersion = assemblyInput;
  }
  public void setAnnotationVersion(String annotationInput) {
    annotationVersion = annotationInput;
  }    
  public void setGenus(String genusInput) {
    genus = genusInput;
  }    
  public void setSpecies(String speciesInput) {
    species = speciesInput;
  }  
  public void setCommonName(String nameInput) {
    commonName = nameInput;
  }  
  public void setShortName(String nameInput) {
    shortName = nameInput;
  }   
  public void setVersion(String versionInput) {
    version = versionInput;
  }   
  public void setName(String nameInput) {
    name = nameInput;
  }   
  public void setProteomeId(String proteomeString) {
    try {
      proteomeId = new Integer(proteomeString);
    } catch (NumberFormatException e) {
      throw new BuildException("Proteome id "+proteomeString+" cannot be parsed as an integer.");
    }
  }
  public void setAssemblyDbxrefId(String assemblyString) {
    try {
      assemblyDbxrefId = new Integer(assemblyString);
    } catch (NumberFormatException e) {
      throw new BuildException("Assembly dbxref_id "+assemblyString+" cannot be parsed as an integer.");
    }
  }
  public void setAnnotationDbxrefId(String annotationString) {
    try {
      annotationDbxrefId = new Integer(annotationString);
    } catch (NumberFormatException e) {
      throw new BuildException("Annotation dbxref_id "+annotationString+" cannot be parsed as an integer.");
    }
  }
  private void setAttributeIfNotNull(Item o,String attribute,String value) {
    if (value != null) {
      o.setAttribute(attribute, value);
    }
  }
}