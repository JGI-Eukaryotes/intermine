package org.intermine.bio.postprocess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

import org.intermine.bio.util.Constants;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.Organism;
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
public class TransferDomainAnnotations {

  private static final Logger LOG = Logger.getLogger(TransferDomainAnnotations.class);
  protected ObjectStore os;
  protected ObjectStoreWriter osw = null;

  /**
   * Create a new UpdateOrthologes object from an ObjectStoreWriter
   * @param osw writer on genomic ObjectStore
   */
  public  TransferDomainAnnotations(ObjectStoreWriter osw) {    
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

    Protein lastProtein = null;

    int count = 0;

    ArrayList<Integer> proteomes = new ArrayList<Integer>();
    HashSet<ProteinDomain> domains = new HashSet<ProteinDomain>();

    Query q = new Query();
    QueryClass qOrg = new QueryClass(Organism.class);
    q.addFrom(qOrg);
    QueryField qF = new QueryField(qOrg,"proteomeId");
    q.addToSelect(qF);
    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Iterator<?>  res = os.execute(q, 100, true, true, true).iterator();
    while(res.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) res.next();
      proteomes.add((Integer)rr.get(0));
    }
    LOG.info("Going to query "+proteomes.size()+" different proteomes.");

    for(Integer proteomeId : proteomes ) {

      osw.beginTransaction();

      // this is an opportune place to hack in an if statement if we know only
      // some things need to be done.
      // if ( proteomeId.intValue() != 324) {

      LOG.info("Making query for "+proteomeId);

      Iterator<?> resIter = findProteinDomains(proteomeId);
      while (resIter.hasNext()  ) {
        ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
        Protein thisProtein = (Protein) rr.get(0);
        ProteinDomain thisDomain = (ProteinDomain) rr.get(1);
        // now process the protein. 

        LOG.info("Adding domain "+thisDomain.getName()+" to protein "+thisProtein.getName());
        if (lastProtein == null) {
          // first pass through
          domains.add(thisDomain);
          lastProtein = thisProtein;
        } else if ( lastProtein.getId() == thisProtein.getId() ) {
          domains.add(thisDomain);
        } else {
          // new protein. Save the old set
          saveDomains(lastProtein,domains);
          domains.clear();
          lastProtein = thisProtein;

        }
        lastProtein = thisProtein;


        if ( (count > 0) && (count%1000 == 0) ) {
          LOG.info("Created "+count+" protein records...");
        }

      }
      // clean up the last one
      saveDomains(lastProtein,domains);

      // end of opportune hack
      //}

      osw.commitTransaction();
    }



    LOG.info("Created " + count + " new Domains objects for Proteins"
        + " - took " + (System.currentTimeMillis() - startTime) + " ms.");

  }

  private void saveDomains(Protein p, HashSet<ProteinDomain> domains) {
    for( ProteinDomain domain : domains) {
      try {
        osw.addToCollection(p.getId(),Protein.class,"proteinDomains",domain.getId());
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing ontologyterm/crossreference: "+e.getMessage());
      }
    }
  }

  /**
   * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene
   *  and GOTerm.
   *
   */
  private Iterator<?> findProteinDomains(Integer proteomeId)
      throws ObjectStoreException {
    Query q = new Query();

    q.setDistinct(true);

    QueryClass qcProtein = new QueryClass(Protein.class);
    q.addFrom(qcProtein);
    q.addToSelect(qcProtein);
    q.addToOrderBy(qcProtein);

    QueryClass qcPAF = new QueryClass(ProteinAnalysisFeature.class);
    q.addFrom(qcPAF);

    QueryClass qcCrossReference= new QueryClass(CrossReference.class);
    q.addFrom(qcCrossReference);

    QueryClass qcProteinDomain = new QueryClass(ProteinDomain.class);
    q.addFrom(qcProteinDomain);
    q.addToSelect(qcProteinDomain);

    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    QueryObjectReference protAnalysisRef = new QueryObjectReference(qcPAF, "protein");
    cs.addConstraint(new ContainsConstraint(protAnalysisRef, ConstraintOp.CONTAINS, qcProtein));

    QueryObjectReference crossRefRef = new QueryObjectReference(qcPAF, "crossReference");
    cs.addConstraint(new ContainsConstraint(crossRefRef, ConstraintOp.CONTAINS, qcCrossReference));

    QueryObjectReference proteinDomainRef = new QueryObjectReference(qcCrossReference, "subject");
    cs.addConstraint(new ContainsConstraint(proteinDomainRef, ConstraintOp.CONTAINS, qcProteinDomain));

    if (proteomeId != null ) {
      QueryClass qcOrganism = new QueryClass(Organism.class);
      QueryField qcProt = new QueryField(qcOrganism,"proteomeId");
      QueryValue qcProtV = new QueryValue(proteomeId);
      QueryObjectReference qcProteinOrgRef = new QueryObjectReference(qcProtein,"organism");
      q.addFrom(qcOrganism);
      cs.addConstraint(new ContainsConstraint(qcProteinOrgRef,ConstraintOp.CONTAINS,qcOrganism));
      cs.addConstraint(new SimpleConstraint(qcProt,ConstraintOp.EQUALS,qcProtV));
    }

    q.setConstraint(cs);
    LOG.info("About to query: "+q);

    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Results res = os.execute(q, 500000, true, true, true);
    return res.iterator();
  }

}
