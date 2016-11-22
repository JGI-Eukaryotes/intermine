package org.intermine.bio.web.displayer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.OrganismPathway;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.PathwayComponent;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;


/**
 * @author jcarlson
 *
 */
public class BiopaxPathwayDisplayer extends ReportDisplayer {

  Profile profile;
  PathQueryExecutor exec;
  String pathwayName = null;

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
  @SuppressWarnings("unchecked")
  public void display(HttpServletRequest request, ReportObject reportObject) {
    HttpSession session = request.getSession();
    final InterMineAPI im = SessionMethods.getInterMineAPI(session);

    profile = SessionMethods.getProfile(session);
    exec = im.getPathQueryExecutor(profile);
    OrganismPathway pathwayObj = (OrganismPathway)reportObject.getObject();
    Organism org = pathwayObj.getOrganism();
    Integer protId = org.getProteomeId();

    ServletContext servletContext = session.getServletContext();
    final Properties webProperties = SessionMethods.getWebProperties(servletContext);

    String json = null;
    HashMap<String,HashSet<String>> enzymes = null;
    HashMap<String,TreeMap<String,Integer>> genes = null;
    try {
      json = getJSON(pathwayObj.getId());
      enzymes = getEnzymes(pathwayObj.getId());
      genes = getGenes(pathwayObj.getId());
    } catch (ObjectStoreException e) {
      return;
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
      // used to calculate the layout. Height is in ex's. Width is in em's
      HashMap<Integer,Integer> maxWidth = new HashMap<Integer,Integer>();
      HashMap<Integer,Integer> maxHeight = new HashMap<Integer,Integer>();
      for(int i=0; i<jNodes.length(); i++) {
        JSONObject node = jNodes.getJSONObject(i);
        Integer x = new Integer(node.getInt("x"));
        maxX = (x>maxX)?x:maxX;
        Integer y = new Integer(node.getInt("y"));
        maxY = (y>maxY)?y:maxY;
        String label = node.getString("label");

        // add genes and enzymes if this is a reaction node
        if (node.getString("type").equals("reaction")) {
          JSONArray ecList = new JSONArray();
          if ( enzymes.containsKey(label) ) {
            for( String enzyme: enzymes.get(label)) {
              ecList.put(enzyme);
            }
          }
          node.put("ecs",ecList);
          JSONArray geneList = new JSONArray();
          if ( genes.containsKey(label) ) {
            List<String> gL = new ArrayList<String>();
            gL.addAll(genes.get(label).keySet());
            Collections.sort(gL);
            for( String gene: gL ) {
              JSONObject jj = new JSONObject();
              jj.put("name",gene);
              geneList.put(jj);
            }
          }
          node.put("genes",geneList);
          node.put("label",label);
        }

        Integer width = getWidth(label);
        Integer height = getHeight(label);
        if (!maxWidth.containsKey(x) || maxWidth.get(x) < width) {
          maxWidth.put(x,width);
        }
        if (!maxHeight.containsKey(y) || maxHeight.get(y) < height) {
          maxHeight.put(y,height);
        }
      }
      // now recompute x and y. For even columns, (0,2,4...) x is the
      // sum of widths <= column number. For odd columns, x is the sum
      // of widths < column number

      // a map from row/col to placement
      HashMap<Integer,Integer> xPlace = new HashMap<Integer,Integer>();
      int xPlacement = 0;
      for(int i = 0 ; i <= maxX; i+=2) {
        if (maxWidth.containsKey(Integer.valueOf(i))) {
          xPlacement += maxWidth.get(Integer.valueOf(i));
        } else {
          // leave a gap?
          xPlacement += 5;
        }
        xPlace.put(Integer.valueOf(i),xPlacement);
        // another gap
        xPlacement += 10;
        xPlace.put(Integer.valueOf(i+1),xPlacement);
        if (maxWidth.containsKey(Integer.valueOf(i+1))) {
          xPlacement += maxWidth.get(Integer.valueOf(i+1));
        } else {
          // leave a gap?
          xPlacement += 5;
        }
      }
      // repeat for y position
      int yPlacement = 0;
      HashMap<Integer,Integer> yPlace = new HashMap<Integer,Integer>();
      for(int i = 0 ; i <= maxY; i++) {
        if (maxHeight.containsKey(Integer.valueOf(i))) {
          yPlacement += maxHeight.get(Integer.valueOf(i));
        } else {
          // leave a gap?
          yPlacement += 2;
        }
        yPlace.put(Integer.valueOf(i),yPlacement);
      }
      for(int i=0; i<jNodes.length(); i++) {
        JSONObject node = jNodes.getJSONObject(i);
        node.put("x",xPlace.get(Integer.valueOf(node.getInt("x"))));
        node.put("y",yPlace.get(Integer.valueOf(node.getInt("y"))));
      }
      addUrlJSON(jObj,webProperties,protId,geneIds);
      request.setAttribute("jsonPathway",jObj.toString());
      if (pathwayName != null) {
        request.setAttribute("pathwayName",pathwayName);
      } else {
        request.setAttribute("pathwayName","unknown");
      }

      JSONObject jE = makeExpressionJSON(pathwayObj.getId());   
      // now get the corresponding expression data
      request.setAttribute("jsonExpression",jE.toString());

      // 
    } catch (Exception e) {
      LOG.warn("Caught an exception: "+e.getMessage());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      LOG.warn("stack trace "+sw.toString());
      // some problem retrieving data or processing the JSON
    }
  }
  private Integer getHeight(String l) {
    // the height of a label. Count <br>'s and add 1
    return new Integer(1 + l.split("<br>").length);
  }

  private Integer getWidth(String l) {
    // the width of a label.
    // first, split on the lines
    String[] lines = l.split("<br>");
    if (lines.length == 1) {
      int width = 0;
      // remove all html tags and sum of the # of characters
      // this over-estimates the width of html escapes (i.e. &alpha;)
      // and UTF-8, but, meh.
      for( String bit:  lines[0].split("<[^>]*>") ) {
        width += bit.length();
      }
      return new Integer(width);
    } else {
      // multiple lines. Look at each and return the longest
      Integer width = new Integer(0);
      for(String line: lines ) {
        Integer newWidth = getWidth(line);
        width = (newWidth > width)?newWidth:width;
      }
      return width;
    }
  }
  private String getJSON(Integer id) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.json","OrganismPathway.pathway.name");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
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
    HashMap<String,HashSet<String>> enzymes = new HashMap<String,HashSet<String>>();
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.identifier","OrganismPathway.pathway.components.ontologyTerms.identifier");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    return makeHash(query);
  }

  private HashMap<String,TreeMap<String,Integer>> getGenes(Integer id) throws ObjectStoreException {
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.identifier",
        "OrganismPathway.pathway.components.proteins.genes.primaryIdentifier",
        "OrganismPathway.pathway.components.proteins.genes.id");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
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
  private JSONObject makeExpressionJSON(Integer id) throws ObjectStoreException, JSONException  {
    // query for the genes and fpkm's associated with the proteins in
    // an organismpathway with this id.
    // this is organized in order of group (first), then gene
    PathQuery query = new PathQuery(im.getModel());
    query.addViews("OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.experimentGroup",
        "OrganismPathway.pathway.components.proteins.genes.primaryIdentifier",
        "OrganismPathway.pathway.components.ontologyTerms.identifier",
        "OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.name",
        "OrganismPathway.pathway.components.proteins.genes.cufflinksscores.fpkm");
    query.addConstraint(Constraints.eq("OrganismPathway.id",id.toString()));
    // probably not necessary to set the order direction...
    query.addOrderBy("OrganismPathway.pathway.components.proteins.genes.cufflinksscores.experiment.experimentGroup", OrderDirection.ASC);
    query.addOrderBy("OrganismPathway.pathway.components.proteins.genes.primaryIdentifier", OrderDirection.ASC);
    // some reactions have no EC's
    query.setOuterJoinStatus("OrganismPathway.pathway.components.ontologyTerms", OuterJoinStatus.OUTER);
    ExportResultsIterator result = exec.execute(query);
    JSONArray jsonArr = new JSONArray();
    String currentGene = null;
    String currentGroup = null;
    // this will hold the results for 1 gene in 1 group
    // the key is the sample name and value the fpkm
    HashMap<String,String> geneResults = null;
    // this will hold the results for all genes in 1 group
    // the key is the gene name and value the geneResults hash
    HashMap<String,HashMap<String,String>> groupResults = null;
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
        geneResults = new HashMap<String,String>();
        groupResults = new HashMap<String,HashMap<String,String>>();
      } else if (!currentGene.equals(gene)) {
        // same group, different gene.
        groupResults.put(currentGene,geneResults);
        geneResults = new HashMap<String,String>();
        currentGene = gene;
      }
      if( row.get(2) != null && row.get(2).getField() != null) {
        ecMap.get(gene).add(row.get(2).getField().toString());
      }else {
        ecMap.get(gene).add("N/A");
      }
      //              |--------- sample name ---------|-------------- fpkm ---------------|
      geneResults.put(row.get(3).getField().toString(),sigFig((Float)row.get(4).getField()));
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

  private void addGroupNode(JSONArray j, String groupName, HashMap<String,HashMap<String,String>> results,HashMap<String,HashSet<String>> ecMap) throws JSONException {
    JSONObject groupNode = new JSONObject();
    groupNode.put("group",groupName);
    JSONArray geneArray = new JSONArray();
    for(String g: results.keySet()) {
      JSONObject geneNode = new JSONObject();
      geneNode.put("gene",g);
      geneNode.put("enzyme",StringUtils.join(ecMap.get(g)," "));
      JSONArray expArray = new JSONArray();
      HashMap<String,String> fpkm = results.get(g);
      for(String e: fpkm.keySet()) {
        JSONObject n = new JSONObject();
        n.put("sample",e);
        n.put("fpkm",fpkm.get(e));
        expArray.put(n);
      }
      geneNode.put("samples",expArray);
      geneArray.put(geneNode);
    }
    groupNode.put("genes",geneArray);
    j.put(groupNode);
  }


  private void addUrlJSON(JSONObject jE,Properties p,Integer orgId,TreeMap<String,Integer> genes) throws ObjectStoreException, JSONException {

    //parse our beautiful json for the identifiers.

    JSONArray jsonArr = new JSONArray();
    // the data
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
}
