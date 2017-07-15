package org.intermine.bio.postprocess;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homolog;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

public class InparanoidHomologsPostProcess extends PostProcessor {

  protected static final Logger LOG =
      Logger.getLogger(InparanoidHomologsPostProcess.class);

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public InparanoidHomologsPostProcess(ObjectStoreWriter writer) {
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
    
    Query q = new Query();
    QueryClass qcHomolog = new QueryClass(Homolog.class);

    q.addFrom(qcHomolog);
    q.addToSelect(qcHomolog);

    q.addToOrderBy(qcHomolog);
    
    ObjectStore os = osw.getObjectStore();

    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Results res = os.execute(q, 500, true, true, true);

    osw.beginTransaction();
    Gene lastGene = null;
    int count = 0;
    Set<Homolog> newCollection = new HashSet<Homolog>();

    Iterator<?> resIter = res.iterator();
    while( resIter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Homolog theHomolog =(Homolog)rr.get(0);
      Gene theGene = theHomolog.getGene();

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
          tempGene.setFieldValue("homolog", newCollection);
          osw.store(tempGene);
          count++;
        }
        newCollection = new HashSet<Homolog>();
      }

      newCollection.add(theHomolog);
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
      tempGene.setFieldValue("homolog", newCollection);
      osw.store(tempGene);
      count++;
    }

    osw.commitTransaction();
    LOG.info("Created " + count + " Gene.homolog collections");
    
  }
  
}
