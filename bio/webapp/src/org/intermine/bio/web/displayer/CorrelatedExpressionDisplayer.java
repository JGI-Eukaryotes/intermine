package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


public class CorrelatedExpressionDisplayer extends ReportDisplayer {

  protected static final Logger LOG = Logger.getLogger(CorrelatedExpressionDisplayer.class);

  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public CorrelatedExpressionDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
      super(config, im);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
      HttpSession session = request.getSession();
      final InterMineAPI im = SessionMethods.getInterMineAPI(session);

      Float threshold = new Float(.85);

      ArrayList<GeneCorrelation> correlationList = new ArrayList<GeneCorrelation>();
      // TODO: what if this is an mRNA?
      BioEntity bioObj = (BioEntity)reportObject.getObject();
      
      LOG.info("Entering CorrelatedExpressionDisplayer.display for "+bioObj.getPrimaryIdentifier());
      LOG.info("Id is "+bioObj.getId());
      
      String bioType = reportObject.getClassDescriptor().getSimpleName();
      LOG.info("Object is a "+bioType);
      
      Profile profile = SessionMethods.getProfile(session);
      PathQueryExecutor exec = im.getPathQueryExecutor(profile);
      
      // get the base fpkm vector
      PathQuery baseQuery = getBaseFPKMQuery(bioObj.getId(),bioObj.getOrganism().getProteomeId());
      ArrayList<Float> baseData = new ArrayList<Float>();
      ExportResultsIterator result;
      try {
        result = exec.execute(baseQuery);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Caught an ObjectStoreException in base query "+e.getMessage());
        return;
      }
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();
        baseData.add((Float)(resElement.get(1).getField()));
      }
      // need multiple points for correlation
      if (baseData.size() < 2 ) return;

      // query the fpkm for the different genes.
      PathQuery query = getCorrelationQuery(bioType,bioObj.getOrganism().getProteomeId());
      // big query. set the batchsize big
      exec.setBatchSize(100000);
 
      try {
        result = exec.execute(query);
      } catch (ObjectStoreException e) {
        // silently return
        LOG.warn("Caught an ObjectStoreException in retrieving data: "+e.getMessage());
        return;
      }
      // normalize the base data.
      // if this returns false, we cannot compute
      if( !normalize(baseData)) {
        return;
      }

      // go through the results and compute correlation

      String lastGeneName = null;
      Integer lastGeneId = null;
      ArrayList<Float> data = new ArrayList<Float>();
      while (result.hasNext()) {
        List<ResultElement> resElement = result.next();

        String thisGeneName = resElement.get(1).getField().toString();
        if (lastGeneName == null || thisGeneName.equals(lastGeneName) ) {
          data.add((Float)(resElement.get(3).getField()));
        } else {
          if( data.size() == baseData.size()) {
            Float correlation = calcCorrelation(baseData,data);
            if (correlation != null && correlation > threshold) correlationList.add(new GeneCorrelation(lastGeneId,lastGeneName,correlation));
          }
          data = new ArrayList<Float>();
          data.add((Float)(resElement.get(3).getField()));
        }
        lastGeneId = (Integer)(resElement.get(0).getField());
        lastGeneName = resElement.get(1).getField().toString();
       }
          
      LOG.info("Found "+correlationList.size()+" correlated genes.");
      Collections.sort(correlationList);
      
      for( GeneCorrelation gC : correlationList) {
        try {
          if (bioType.equals("Gene")) {
            Gene g = (Gene)im.getObjectStore().getObjectById(gC.getId());
            gC.setDefline(g.getBriefDescription()==null?
                "&nbsp;":g.getBriefDescription());
          } else if (bioType.equals("Transcript") || bioType.equals("MRNA")) {
            Transcript t = (Transcript)im.getObjectStore().getObjectById(gC.getId());
            gC.setDefline(t.getGene().getBriefDescription());
          }
        } catch (ObjectStoreException e) {
          LOG.warn("ObjectStore exception when trying to get defline: "+e.getMessage());
        }
      }
      request.setAttribute("type",bioType.toLowerCase()+((correlationList.size()>1)?"s":""));
      // we'll want this for creating a list (in case there are duplicates)
      request.setAttribute("organism",bioObj.getOrganism().getProteomeId());
      request.setAttribute("threshold",threshold);
      request.setAttribute("list",correlationList);
      request.setAttribute("id",bioObj.getId());
      
  }

  PathQuery getBaseFPKMQuery(Integer bioId,Integer proteomeId) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "CufflinksScore.experiment.name","CufflinksScore.fpkm");
    query.addOrderBy("CufflinksScore.experiment.name", OrderDirection.ASC);
    query.addConstraint(Constraints.eq("CufflinksScore.bioentity.id",bioId.toString()));
    query.addConstraint(Constraints.isNotNull("CufflinksScore.fpkm"));
    // hardcoded hack
    if (proteomeId.equals(281)) {
      query.addConstraint(Constraints.eq("CufflinksScore.experiment.experimentGroup", "GeneAtlas"));
    }
    return query;
  }

  private PathQuery getCorrelationQuery(String bioType,Integer proteomeId) {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews( "CufflinksScore.bioentity.id",
        "CufflinksScore.bioentity.primaryIdentifier",
        "CufflinksScore.experiment.name","CufflinksScore.fpkm");
    query.addOrderBy("CufflinksScore.bioentity.primaryIdentifier", OrderDirection.ASC);
    query.addOrderBy("CufflinksScore.experiment.name", OrderDirection.ASC);
    query.addConstraint(Constraints.isNotNull("CufflinksScore.fpkm"));
    query.addConstraint(Constraints.eq("CufflinksScore.bioentity.organism.proteomeId",proteomeId.toString()));
    query.addConstraint(Constraints.type("CufflinksScore.bioentity",bioType));
    // hardcoded hack
    if (proteomeId.equals(281)) {
      query.addConstraint(Constraints.eq("CufflinksScore.experiment.experimentGroup", "GeneAtlas"));
    }
    return query;
  }
  
  private boolean normalize(ArrayList<Float> data) {
    double sum1 = 0;
    double sum2 = 0;
    int ctr = 0;
    for( Float x : data ) {
      if (x==null) {
        LOG.warn("Had a null value in data.");
        return false;
      }
      if (x == 0.) {
        x = new Float(-20);
      } else {
        x = new Float(Math.log(x.doubleValue())/Math.log(2.));
      }
      data.set(ctr,x);
      sum1 += x;
      sum2 += x*x;
      ctr++;
    }
    sum1 = sum1/data.size();
    sum2 = Math.sqrt(sum2-sum1*sum1*data.size());
    if( sum2 == 0.) {
      return false;
    }
    for(int i=0;i<data.size();i++) {
      data.set(i,new Float((data.get(i)-sum1)/sum2));
    }
    return true;
  }
  private Float calcCorrelation(ArrayList<Float> base, ArrayList<Float> data) {
    if (normalize(data)) {
      Float sum = new Float(0.);
      for(int i=0;i<base.size();i++) {
        sum += base.get(i)*data.get(i);
      }
      return sum;
    } else {
      return null;
    }
  }

  public class GeneCorrelation implements Comparable {
    private Integer id;
    private String geneName;
    private String defline;
    private Float correlation;
    public GeneCorrelation(Integer id,String geneName, Float correlation,String defline) {
      this.id = id;
      this.geneName = geneName;
      this.correlation = correlation;
      this.defline=defline;
    }
    public GeneCorrelation(Integer id,String geneName, Float correlation) {
      this.id = id;
      this.geneName = geneName;
      this.correlation = correlation;
      this.defline=null;
    }
    public Integer getId() { return id; }
    public String getGeneName() { return geneName; }
    public Float getCorrelation() { return correlation; }
    public String getDefline() { return defline; };
    private void setDefline(String defline) { this.defline = defline; }
    public int compareTo(Object other) {
        return -correlation.compareTo(((GeneCorrelation)other).getCorrelation());
    }
  }
}
