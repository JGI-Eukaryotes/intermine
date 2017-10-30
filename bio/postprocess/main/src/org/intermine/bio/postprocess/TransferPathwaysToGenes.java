package org.intermine.bio.postprocess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

import org.intermine.bio.util.Constants;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinDomain;
import org.intermine.model.bio.ProteinAnalysisFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Take any GOAnnotation objects assigned to proteins and copy them to corresponding genes and proteins.
 * The GO terms are stored slightly differently. At the gene level, they are GOAnnotations. The
 * proteins are stored as OntologyAnnotations.
 *
 * Modified for phytozome schema from GoPostprocess
 * 
 * @author J Carlson 
 */

public class TransferPathwaysToGenes {

  private static final Logger LOG = Logger.getLogger(TransferPathwaysToGenes.class);
  protected ObjectStore os;
  protected ObjectStoreWriter osw = null;

  /**
   * Create a new UpdateOrthologes object from an ObjectStoreWriter
   * @param osw writer on genomic ObjectStore
   */
  public  TransferPathwaysToGenes(ObjectStoreWriter osw) {    
    this.osw = osw;
    this.os = osw.getObjectStore();
  }

  /**
   * Copy all Protein Domains annotations from the ProteinAnalysisFeature cross reference objects 
   * to the corresponding Protein(s)
   * @throws ObjectStoreException if anything goes wrong
   */
  public void execute() throws ObjectStoreException {

    long startTime = System.currentTimeMillis();
    // we need to associate genes with the pathways.
    LOG.info("In the postprocessor.");
    
    Query q = new Query();
    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
    QueryClass qcGene = new QueryClass(Gene.class);
    QueryClass qcPathway = new QueryClass(Pathway.class);
    QueryClass qcProtein = new QueryClass(Protein.class);

    q.addFrom(qcGene);
    q.addToSelect(qcGene);
    q.addFrom(qcPathway);
    q.addToSelect(qcPathway);
    q.addFrom(qcProtein);

    q.addToOrderBy(qcPathway);
    
    QueryCollectionReference pathwayProtein = new QueryCollectionReference(qcPathway, "proteins");
    cs.addConstraint(new ContainsConstraint(pathwayProtein, ConstraintOp.CONTAINS, qcProtein));
    
    QueryCollectionReference proteinGene = new QueryCollectionReference(qcProtein,"genes");
    cs.addConstraint(new ContainsConstraint(proteinGene, ConstraintOp.CONTAINS, qcGene));

    q.setConstraint(cs);
    ObjectStore os = osw.getObjectStore();

    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Results res = os.execute(q, 500, true, true, true);

    osw.beginTransaction();
    Gene lastGene = null;
    int count = 0;
    Set<Pathway> newCollection = new HashSet<Pathway>();

    Iterator<?> resIter = res.iterator();
    while( resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Gene theGene =(Gene)rr.get(0);
      Pathway thePathway = (Pathway) rr.get(1);

      if (lastGene == null || !theGene.getId().equals(lastGene.getId())) {
        if (lastGene != null) {
          // clone so we don't change the ObjectStore cache
          Gene tempGene;
          try {
            tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
          } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to clone InterMineObject: "
                + lastGene, e);
          }
          tempGene.setFieldValue("pathways", newCollection);
          osw.store(tempGene);
          count++;
        }
        newCollection = new HashSet<Pathway>();
      }

      newCollection.add(thePathway);
      lastGene = theGene;
    }

    // last one
    if (lastGene != null) {
      // clone so we don't change the ObjectStore cache
      Gene tempGene;
      try {
        tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to clone InterMineObject: " + lastGene, e);
      }
      tempGene.setFieldValue("pathways", newCollection);
      osw.store(tempGene);
      count++;
    }

    osw.commitTransaction();
    LOG.info("Created " + count + " Gene.pathways collections");
    
  }

}

