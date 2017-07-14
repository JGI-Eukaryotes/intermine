package org.intermine.bio.postprocess;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;

public class BiopaxPathwayPostProcess extends PostProcessor {

  protected String organismPrefix = null;
  protected Integer proteomeId = null;
  
  protected static final Logger LOG =
      Logger.getLogger(BiopaxPathwayPostProcess.class);

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public BiopaxPathwayPostProcess(ObjectStoreWriter writer) {
    super(writer);
  }

  /**
   *
   *
   * {@inheritDoc}
   */
  public void postProcess() throws BuildException, ObjectStoreException {
    // we need to associate genes with the pathways.
    LOG.info("In the postprocessor.");
    // if this is null, then there is nothing to do.
    if (proteomeId==null) return;
    
    Query q = new Query();
    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
    QueryClass qcOrganism = new QueryClass(Organism.class);
    QueryClass qcGene = new QueryClass(Gene.class);
    QueryClass qcPathway = new QueryClass(Pathway.class);
    QueryClass qcProtein = new QueryClass(Protein.class);

    q.addFrom(qcGene);
    q.addToSelect(qcGene);
    q.addFrom(qcPathway);
    q.addToSelect(qcPathway);
    q.addFrom(qcOrganism);
    q.addFrom(qcProtein);

    q.addToOrderBy(qcPathway);
    
    QueryCollectionReference pathwayProtein = new QueryCollectionReference(qcPathway, "proteins");
    cs.addConstraint(new ContainsConstraint(pathwayProtein, ConstraintOp.CONTAINS, qcProtein));
    
    QueryCollectionReference proteinGene = new QueryCollectionReference(qcProtein,"genes");
    cs.addConstraint(new ContainsConstraint(proteinGene, ConstraintOp.CONTAINS, qcGene));

    QueryField qfProteome = new QueryField(qcOrganism,"proteomeId");
    QueryValue qvProteome = new QueryValue(proteomeId);
    cs.addConstraint(new SimpleConstraint(qfProteome, ConstraintOp.EQUALS, qvProteome));
    
    QueryObjectReference pathwayOrganism = new QueryObjectReference(qcPathway,"organism");
    cs.addConstraint(new ContainsConstraint(pathwayOrganism, ConstraintOp.CONTAINS, qcOrganism));

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
  
  public void setOrganismPrefix(String s) {
    this.organismPrefix = s;
  }

  public void setProteomeId(String proteomeId) throws ObjectStoreException {
    try {
      this.proteomeId = new Integer(proteomeId);
    } catch (NumberFormatException e) {
      throw new BuildException("proteomeId is not an integer.");
    }
  }
}
