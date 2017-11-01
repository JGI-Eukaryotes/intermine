package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Pathway;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author jcarlson
 *
 */
public class BiopaxPathwayDisplayer extends ReportDisplayer {

  Profile profile;
  PathQueryExecutor exec;
  String pathwayName = null;
  Organism org = null;

  protected static final Logger LOG = Logger.getLogger(BiopaxPathwayDisplayer.class);
  /**
   * Construct with config and the InterMineAPI.
   * @param config to describe the report displayer
   * @param im the InterMine API
   */
  public BiopaxPathwayDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
    super(config, im);
  }

  @Override
  public void display(HttpServletRequest request, ReportObject reportObject) {
    HttpSession session = request.getSession();
    final InterMineAPI im = SessionMethods.getInterMineAPI(session);

    profile = SessionMethods.getProfile(session);
    exec = im.getPathQueryExecutor(profile);
    Pathway pathwayObj = (Pathway)reportObject.getObject();
    org = pathwayObj.getOrganism();
    Integer protId = org.getProteomeId();

    ServletContext servletContext = session.getServletContext();
    final Properties webProperties = SessionMethods.getWebProperties(servletContext);

    JSONObject jP = makePathwayJSON(pathwayObj,webProperties,protId);
    // there was a problem when making the json....
    if (jP == null) return;

    request.setAttribute("jsonPathway",jP.toString());
    if (pathwayName != null) {
      request.setAttribute("pathwayName",pathwayName);
    } else {
      request.setAttribute("pathwayName","unknown");
    }

    try {
      // now get the corresponding expression data
      JSONObject jE = makeExpressionJSON(pathwayObj.getId(),org);
      request.setAttribute("jsonExpression",jE.toString());
      // if this throws an exception, we'll still display the pathway
    } catch (ObjectStoreException e) {
      LOG.warn("Caught an objectstore exception when constructing expression: "+e.getMessage());
    } catch (JSONException e) {
      LOG.warn("Caught a json exception when constructing expression: "+e.getMessage());
    }


    try {
      JSONArray jOo = makeOtherOrgJSON(pathwayObj,webProperties,protId);
      request.setAttribute("jsonLinks",jOo.toString());
    } catch (ObjectStoreException e) {
      LOG.warn("Caught an objectstore exception when constructing links: "+e.getMessage());
    } catch (JSONException e) {
      LOG.warn("Caught a json exception when constructing links: "+e.getMessage());
    }
  }

  private JSONObject makePathwayJSON(Pathway pathwayObj,Properties webProperties,Integer protId) {

    String json = null;
    HashMap<String,HashSet<String>> enzymes = null;
    HashMap<String,TreeMap<String,Integer>> genes = null;
    // first, extract the template JSON and enzyme/gene components.
    try {
      json = getJSONRecord(pathwayObj.getId());
      enzymes = getEnzymes(pathwayObj.getId());
      genes = getGenes(pathwayObj.getId(),org);
    } catch (ObjectStoreException e) {
      return null;
    }

    // this is a simplified map of gene -> intermine id
    TreeMap<String,Integer> geneIds = new TreeMap<String,Integer>();
    for(String label: genes.keySet() ) {
      for(String gene: genes.get(label).keySet() ) {
        geneIds.put(gene,genes.get(label).get(gene));
      }
    }

    try {
      // now we need to process the pathway json template. Add the enzyme/proteins
      // and calculate positions.
      JSONObject jObj = new JSONObject(json);
      JSONArray jNodes = jObj.getJSONArray("nodes");

      int maxX = 0;
      int maxY = 0;
      for(int i=0; i<jNodes.length(); i++) {
        JSONObject node = jNodes.getJSONObject(i);
        Integer x = new Integer(node.getInt("x"));
        maxX = (x>maxX)?x:maxX;
        Integer y = new Integer(node.getInt("y"));
        maxY = (y>maxY)?y:maxY;
        String key = node.getString("key");

        // add genes and enzymes if this is a reaction node
        if (node.getString("type").equals("reaction")) {
          JSONArray ecList = new JSONArray();
          if ( enzymes.containsKey(key) ) {
            for( String enzyme: enzymes.get(key)) {
              ecList.put(enzyme);
            }
          }
          node.put("ecs",ecList);
          JSONArray geneList = new JSONArray();
          if ( genes.containsKey(key) ) {
            List<String> gL = new ArrayList<String>();
            gL.addAll(genes.get(key).keySet());
            Collections.sort(gL);
            for( String gene: gL ) {
              JSONObject jj = new JSONObject();
              jj.put("name",gene);
              geneList.put(jj);
            }
          }
          node.put("genes",geneList);
        }
      }

      addUrlJSON(jObj,webProperties,protId,geneIds);
      return jObj;
    } catch (JSONException e) {
      return null;
    } catch (ObjectStoreException e) {
      return null;
    }
  }


  private String getJSONRecord(Integer id) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("PathwayJSON.json","PathwayJSON.pathwayInfo.name");
    query.addConstraint(Constraints.eq("PathwayJSON.pathwayInfo.pathways.id",id.toString()));
    ExportResultsIterator result = exec.execute(query);
    String json = null;
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      json = row.get(0).getField().toString();
      pathwayName = row.get(1).getField().toString();
    }
    return json;
  }

  private HashMap<String,HashSet<String>> getEnzymes(Integer id) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("PathwayInfo.components.key","PathwayInfo.components.ontologyTerms.identifier");
    query.addConstraint(Constraints.eq("PathwayInfo.pathways.id",id.toString()));
    return makeHash(query);
  }

  private HashMap<String,TreeMap<String,Integer>> getGenes(Integer id,Organism org) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("PathwayInfo.components.key",
        "PathwayInfo.components.proteins.genes.primaryIdentifier",
        "PathwayInfo.components.proteins.genes.id");
    query.addConstraint(Constraints.eq("PathwayInfo.pathways.id",id.toString()));
    query.addConstraint(Constraints.eq("PathwayInfo.components.proteins.organism.proteomeId",
        Integer.toString(org.getProteomeId())));
    return makeHashPair(query);
  }

  private HashMap<String,HashSet<String>> makeHash(PathQuery q) throws ObjectStoreException {
    ExportResultsIterator result = exec.execute(q);
    HashMap<String,HashSet<String>> map = new HashMap<String,HashSet<String>>();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String node = row.get(0).getField().toString();
      if (!map.containsKey(node)) {
        map.put(node,new HashSet<String>());
      }
      map.get(node).add(row.get(1).getField().toString());
    }
    return map;
  }

  private HashMap<String,TreeMap<String,Integer>> makeHashPair(PathQuery q) throws ObjectStoreException {
    ExportResultsIterator result = exec.execute(q);
    HashMap<String,TreeMap<String,Integer>> map = new HashMap<String,TreeMap<String,Integer>>();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String node = row.get(0).getField().toString();
      if (!map.containsKey(node)) {
        map.put(node,new TreeMap<String,Integer>());
      }
      map.get(node).put(row.get(1).getField().toString(),(Integer)row.get(2).getField());
    }
    return map;
  }

  private JSONObject makeExpressionJSON(Integer id,Organism org) throws ObjectStoreException, JSONException  {
    // query for the genes and fpkm's associated with the proteins in
    // a pathway with this id.
    // this is organized in order of group (first), then gene
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("PathwayInfo.components.proteins.genes.rnaSeqExpressions.experiment.experimentGroup",
        "PathwayInfo.components.proteins.genes.primaryIdentifier",
        "PathwayInfo.components.ontologyTerms.identifier",
        "PathwayInfo.components.proteins.genes.rnaSeqExpressions.experiment.name",
        "PathwayInfo.components.proteins.genes.rnaSeqExpressions.abundance",
        "PathwayInfo.components.proteins.genes.rnaSeqExpressions.libraryExpressionLevel",
        "PathwayInfo.components.proteins.genes.rnaSeqExpressions.locusExpressionLevel"
        );
    query.addConstraint(Constraints.eq("PathwayInfo.pathways.id",id.toString()));
    query.addConstraint(Constraints.eq("PathwayInfo.components.proteins.genes.rnaSeqExpressions.method","cufflinks"));
    query.addConstraint(Constraints.eq("PathwayInfo.components.proteins.organism.proteomeId",
        Integer.toString(org.getProteomeId())));
    // probably not necessary to set the order direction...
    query.addOrderBy("PathwayInfo.components.proteins.genes.rnaSeqExpressions.experiment.experimentGroup", OrderDirection.ASC);
    query.addOrderBy("PathwayInfo.components.proteins.genes.primaryIdentifier", OrderDirection.ASC);
    // some reactions have no EC's
    query.setOuterJoinStatus("PathwayInfo.components.ontologyTerms", OuterJoinStatus.OUTER);
    ExportResultsIterator result = exec.execute(query);
    JSONArray jsonArr = new JSONArray();
    String currentGene = null;
    String currentGroup = null;
    // this will hold the results for 1 gene in 1 group
    // the key is the sample name and value the fpkm
    HashMap<String,ArrayList<String>> geneResults = null;
    // this will hold the results for all genes in 1 group
    // the key is the gene name and value the geneResults hash
    HashMap<String,HashMap<String,ArrayList<String>>> groupResults = null;
    // a map of gene name to EC.
    HashMap<String,HashSet<String>> ecMap = new HashMap<String,HashSet<String>>();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String group = row.get(0).getField().toString();
      String gene = row.get(1).getField().toString();

      if (!ecMap.containsKey(gene)) ecMap.put(gene,new HashSet<String>()); 
      if (currentGroup==null || !currentGroup.equals(group)) {
        // this is either the first pass through or different group
        if (currentGroup != null) {
          // different group. Put the gene results in the group results
          groupResults.put(currentGene,geneResults);
          // and add all old results to the JSON
          addGroupNode(jsonArr,currentGroup,groupResults,ecMap);
        }
        currentGene = gene;
        currentGroup = group;
        geneResults = new HashMap<String,ArrayList<String>>();
        groupResults = new HashMap<String,HashMap<String,ArrayList<String>>>();
      } else if (!currentGene.equals(gene)) {
        // same group, different gene.
        groupResults.put(currentGene,geneResults);
        geneResults = new HashMap<String,ArrayList<String>>();
        currentGene = gene;
      }
      if( row.get(2) != null && row.get(2).getField() != null) {
        ecMap.get(gene).add(row.get(2).getField().toString());
      }else {
        ecMap.get(gene).add("N/A");
      }
      //              |--------- sample name ---------|
      geneResults.put(row.get(3).getField().toString(),new ArrayList<String>());
      //                                                |-------------- fpkm ---------------|
      geneResults.get(row.get(3).getField().toString()).add(sigFig((Float)row.get(4).getField()));
      //                                                |-------------- library ------------>
      geneResults.get(row.get(3).getField().toString()).add((row.get(5)==null || row.get(5).getField()==null)?"":row.get(5).getField().toString());
      //                                                |-------------- locus -------------->
      geneResults.get(row.get(3).getField().toString()).add((row.get(6)==null || row.get(6).getField()==null)?"":row.get(6).getField().toString());
    }
    if(geneResults != null && geneResults.keySet() != null && geneResults.keySet().size() > 0) {
      // final insertion
      groupResults.put(currentGene,geneResults);
      // and add all old results to the JSON
      addGroupNode(jsonArr,currentGroup,groupResults,ecMap);
    }
    JSONObject jj = new JSONObject();
    jj.put("data",jsonArr);
    return jj;
  }

  private String sigFig(Float a) {
    // convert a Float to 3 significant figures
    // small is small. Just call it 0
    if ( Math.abs(a) < 1e-6) return "0.00";
    long leftWidth = Math.round(Math.ceil(Math.log10(Math.abs(a))));
    if (leftWidth < 4) {
      return String.format("%1."+(3-leftWidth)+"f",a);
    } else {
      return String.format("%1.0f",a);
    }
  }

  private void addGroupNode(JSONArray j, String groupName, HashMap<String,HashMap<String,ArrayList<String>>> results,
      HashMap<String,HashSet<String>> ecMap) throws JSONException {
    JSONObject groupNode = new JSONObject();
    groupNode.put("group",groupName);
    JSONArray geneArray = new JSONArray();
    for(String g: results.keySet()) {
      JSONObject geneNode = new JSONObject();
      geneNode.put("gene",g);
      JSONArray ecArray = new JSONArray();
      for(String ec: ecMap.get(g)) {
        ecArray.put(ec);
      }
      geneNode.put("enzyme",ecArray);
      JSONArray expArray = new JSONArray();
      HashMap<String,ArrayList<String>> fpkm = results.get(g);
      for(String e: fpkm.keySet()) {
        JSONObject n = new JSONObject();
        n.put("sample",e);
        n.put("fpkm",fpkm.get(e).toArray()[0]);
        n.put("library",fpkm.get(e).toArray()[1]);
        n.put("locus",fpkm.get(e).toArray()[2]);
        expArray.put(n);
      }
      geneNode.put("samples",expArray);
      geneArray.put(geneNode);
    }
    groupNode.put("genes",geneArray);
    j.put(groupNode);
  }


  private void addUrlJSON(JSONObject jE,Properties p,Integer orgId,TreeMap<String,Integer> genes) throws ObjectStoreException, JSONException {

    //parse our beautiful json for the identifiers and add link URLs.
    // the node data
    JSONArray jData = jE.getJSONArray("nodes");

    // hack
    String jBTem = p.getProperty("attributelink.JBrowse.Gene."+orgId+".chromosomeLocation.paddedRegion.url");
    String pWTem = p.getProperty("attributelink.Phytozome.Gene."+orgId+".primaryIdentifier.url");
    String pMTem = p.getProperty("webapp.path");

    for( int i = 0; i < jData.length(); i++) {
      JSONObject jNode = jData.getJSONObject(i);
      if (jNode.get("type").equals("reaction") && jNode.has("genes") )  {
        JSONArray geneArray = jNode.getJSONArray("genes");
        for( int j=0; j<geneArray.length(); j++) {
          JSONObject jj = geneArray.getJSONObject(j);
          JSONArray jLinks = new JSONArray();
          if ( genes.containsKey(jj.get("name")) && pMTem != null ) {
            jLinks.put(makeJL("Phytomine Gene Report","/"+pMTem+"/report.do?id="+genes.get(jj.get("name"))));
          }
          if (pWTem != null) {{
            jLinks.put(makeJL("PhytoWeb Gene Report",pWTem.replace("<<attributeValue>>",(String)jj.getString("name"))));
          }
          if (jBTem != null){
            jLinks.put(makeJL("View in jBrowse",jBTem.replace("<<attributeValue>>",jj.getString("name"))));
          }
          jj.put("links",jLinks);
          }
        }
      }
    }
  }

  private JSONObject makeJL(String a, String b) throws JSONException {
    JSONObject jl = new JSONObject();
    jl.put("label",a);
    jl.put("url",b);
    return jl;
  }

  private JSONArray makeOtherOrgJSON(Pathway pathway,Properties p,Integer id) throws ObjectStoreException, JSONException  {
    String pMTem = p.getProperty("webapp.path");

    PathQuery query = new PathQuery(im.getModel());
    query.addViews("Pathway.organism.shortName",
        "Pathway.id"
        );
    query.addConstraint(Constraints.eq("Pathway.pathwayInfo.id",pathway.getPathwayInfo().getId().toString()));
    query.addConstraint(Constraints.neq("Pathway.organism.id",pathway.getOrganism().getId().toString()));
    ExportResultsIterator result = exec.execute(query);

    JSONArray jLinks = new JSONArray();
    while (result.hasNext()) {
      List<ResultElement> row = result.next();
      String orgName = row.get(0).getField().toString();
      Integer pId = (Integer)row.get(1).getField();
      jLinks.put(makeJL(orgName,"/"+pMTem+"/report.do?id="+pId));
    }
    return jLinks;
  }
}
