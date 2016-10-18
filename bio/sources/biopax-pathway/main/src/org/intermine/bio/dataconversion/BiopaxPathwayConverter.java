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

import java.io.Reader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.Traverser;
import org.biopax.paxtools.controller.Visitor;
import org.biopax.paxtools.impl.level3.BiochemicalPathwayStepImpl;
import org.biopax.paxtools.impl.level3.PathwayStepImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BiochemicalPathwayStep;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.StepDirection;
import org.biopax.paxtools.model.level3.Transport;
import org.biopax.paxtools.model.level3.Xref;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;


/**
 *
 * @author
 */
public class BiopaxPathwayConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "Pathologic Pathway";
  private static final String DATA_SOURCE_NAME = "Pathologic";

  private Integer proteomeId = null;
  private String method = null;
  private Item organism = null;
  HashSet<String> beenThereDoneThat = null;
  protected static final Logger LOG =
      Logger.getLogger(BiopaxPathwayConverter.class);

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public BiopaxPathwayConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
  }

  /**
   *
   *
   * {@inheritDoc}
   */
  public void process(Reader reader) throws ObjectStoreException {

    // we may want to relax this constraint at some point in
    // order to load pathway info independed of organism. But
    // untill then, we demand an organism
    if (organism==null) {
      throw new BuildException("Organism must be set to load pathways.");
    }

    SimpleIOHandler handler = new SimpleIOHandler(BioPAXLevel.L3);
    org.biopax.paxtools.model.Model model = handler.convertFromOWL(new ReaderInputStream(reader));
    Set<Pathway> pathwaySet = model.getObjects(Pathway.class);

    // record the EC ontology and make a map to store the observed entries
    Item ontology = createItem("Ontology");
    ontology.setAttribute("name","ENZYME ");
    store(ontology);
    // hashes for enzymes and proteins that we register along the way.
    TreeMap<String,Item> enzymeHash = new TreeMap<String,Item>();
    TreeMap<String,Item> proteinHash = new TreeMap<String,Item>();

    for (Pathway pathwayObj : pathwaySet) {

      String pathwayID = null;
      String pathwayName = ((Named)pathwayObj).getStandardName();
      for ( Xref xref: pathwayObj.getXref() ) {
        pathwayID = xref.getId();
      }
      LOG.info("Processing current pathway id "+pathwayID+" name is "+pathwayName);

      Item pathway = createItem("Pathway");
      if (pathwayID != null) pathway.setAttribute("identifier", pathwayID);
      if (pathwayName != null) pathway.setAttribute("name", pathwayName);
      // the first walk of the pathway just gets the reactions
      TreeMap<String,Node> nodeMap = getReactionSteps(pathwayObj,pathwayID);

      // we'll need the initial nodes.
      // First, determine the reverse links
      for(String node: nodeMap.keySet()) {
        if ( nodeMap.get(node) instanceof ReactionNode) {
          for( String n: ((ReactionNode)nodeMap.get(node)).nextNode) {
            ((ReactionNode)nodeMap.get(n)).prevNode.add(node);
          }
        }
      }
      // and use this to find the initial node(s)
      TreeSet<String> initialNodes = new TreeSet<String>();
      TreeSet<String> terminalNodes = new TreeSet<String>();
      for(String node: nodeMap.keySet()) {
        if ( nodeMap.get(node) instanceof ReactionNode) {
          if (((ReactionNode)nodeMap.get(node)).nextNode.size() == 0) {
            terminalNodes.add(node);
          }
          if (((ReactionNode)nodeMap.get(node)).prevNode.size() == 0) {
            initialNodes.add(node);
          }
        }
      }
      // then we need to walk the paths of the reactions to
      // get the substrates and outputs starting from (all?) initial nodes.
      // As we walk the reactions, we sweep up the components into either
      // output nodes from the previous reaction node, input nodes to
      // the current reaction node, or linking nodes of outputs of the previous
      // which are also inputs for the current.

      HashSet<Node[]> linkSet = new HashSet<Node[]>();
      if(initialNodes.size() > 0) {
        // we need to track node pairs we've visited to avoid double tracking
        beenThereDoneThat= new HashSet<String>();

        // this will be used for the row of the node
        // we set this to 1 (rather than 0) to have a
        // row for the inputs.
        Integer rowI = new Integer(1);
        // and this will be the column.
        Integer columnI = new Integer(0);
        // TODO: look for problems with this algorithm. If there are
        // multiple branches in a walk, the second will cross the column
        // created by the first. This is a problem.
        for( String initial: initialNodes) {
          ReactionNode prevNode = null;
          ReactionNode currentNode = (ReactionNode) nodeMap.get(initial);
          // this walk is recursive
          walk(rowI,columnI,(ReactionNode)prevNode,(ReactionNode)currentNode,linkSet,nodeMap);
          columnI = new Integer(maxColumn(nodeMap) + 1);
        }

        // Time to store
        // first the components
        TreeSet<Item> components = new TreeSet<Item>();
        for(String key: nodeMap.keySet() ) {
          Node activeNode = nodeMap.get(key);
          LOG.info("Component key is "+key+" with row="+activeNode.row()+" and column "+activeNode.column());
          Item component = createItem("PathwayComponent");
          component.setAttribute("identifier",key);
          if( activeNode.label() != null && activeNode.label().length() > 0) component.setAttribute("name",activeNode.label());
          if ( activeNode.row() != null) component.setAttribute("step",activeNode.row().toString());
          if ( activeNode.column() != null) component.setAttribute("level",activeNode.column().toString());
          if (activeNode instanceof InputNode) {
            component.setAttribute("type","input");
          } else if (activeNode instanceof OutputNode) {
            component.setAttribute("type","output");
          } else if (activeNode instanceof LinkingNode) {
            component.setAttribute("type","link");
          } else if(activeNode instanceof ReactionNode) {
            component.setAttribute("type","reaction");
            for( String protein: ((ReactionNode)activeNode).proteins ) {
              if (!proteinHash.containsKey(protein) ) {
                Item p = createItem("Protein");
                p.setAttribute("primaryIdentifier",protein);
                p.setReference("organism",organism);
                store(p);
                proteinHash.put(protein,p);
              }
              component.addToCollection("proteins",proteinHash.get(protein));
            }
            for( String ec: ((ReactionNode)activeNode).ec ) {
              if (!enzymeHash.containsKey(ec) ) {
                Item e = createItem("OntologyTerm");
                e.setAttribute("identifier",ec);
                e.setReference("ontology",ontology);
                store(e);
                enzymeHash.put(ec,e);
              }
              component.addToCollection("ontologyTerms",enzymeHash.get(ec));
            }
          } else if (activeNode instanceof SpontaneousNode) {
            component.setAttribute("type","spontaneous");
          } else {
            throw new BuildException("Unknow node type is this node? "+key+" "+nodeMap.get(key));
          }
          components.add(component);
        }

        // Now build the json.
        // this connects the node key to an integer
        TreeMap<String,Integer> nodeToId = new TreeMap<String,Integer>();
        Integer currentId = new Integer(0);
        // various buffers for holding things.
        StringBuffer nodes = new StringBuffer();
        StringBuffer links = new StringBuffer();
        StringBuffer groups = new StringBuffer();


        for( String key : nodeMap.keySet()) {
          Node activeNode = nodeMap.get(key);
          if( activeNode.column() != null && activeNode.row() != null) {
            nodeToId.put(key,currentId);
            if (activeNode instanceof InputNode) {
              if (nodes.length() > 0 ) nodes.append(",");
              nodes.append("{\"id\":"+currentId +
                  ",\"label\":\""+activeNode.label+"\"" +
                  ",\"x\":"+activeNode.column() +
                  ",\"y\":"+activeNode.row() +
                  ",\"type\":\"input\""+
                  "}");
            } else if (activeNode instanceof OutputNode) {
              if (nodes.length() > 0 ) nodes.append(",");
              nodes.append("{\"id\":"+currentId +
                  ",\"label\":\""+activeNode.label+"\"" +
                  ",\"x\":"+activeNode.column() +
                  ",\"y\":"+activeNode.row() +
                  ",\"type\":\"output\""+
                  "}");
            } else if (activeNode instanceof LinkingNode) {
              if (nodes.length() > 0 ) nodes.append(",");
              nodes.append("{\"id\":"+currentId +
                  ",\"label\":\""+activeNode.label+"\"" +
                  ",\"x\":"+activeNode.column() +
                  ",\"y\":"+activeNode.row() +
                  ",\"type\":\"link\""+
                  "}");
            } else if(activeNode instanceof ReactionNode) {
              if (nodes.length() > 0 ) nodes.append(",");
              ReactionNode rn = (ReactionNode)activeNode;
              nodes.append("{\"id\":"+currentId+",\"label\":\""+rn.label()+
                  "\",\"tooltip\":\""+activeNode.info()+
                  "\",\"x\":"+activeNode.column() +
                  ",\"y\":"+activeNode.row() +
                  ",\"type\":\"reaction\""+"}");

            } else if (activeNode instanceof SpontaneousNode) {
              if (nodes.length() > 0 ) nodes.append(",");
              nodes.append("{\"id\":"+currentId +
                  ",\"label\":\""+activeNode.label+"\"" +
                  ",\"x\":"+activeNode.column() +
                  ",\"y\":"+activeNode.row() +
                  ",\"type\":\"spontaneous\""+
                  "}");
            } else {
              throw new BuildException("What is this node? "+key+" "+activeNode);
            }
            currentId = currentId + 1;
          } else {
            LOG.warn("There are untraveled reactions in "+pathwayID);
          }
        }

        for( Node[] pair: linkSet) {
          if (links.length() > 0) links.append(",");
          links.append("{\""+pair[0].linkType("source")+"\":"+nodeToId.get(pair[0].uniqueName)+
              ",\""+pair[1].linkType("target")+"\":"+nodeToId.get(pair[1].uniqueName)+"}");

        }

        // keekp track of who is in a group. We'll construct groups
        // of single elements for those not otherwise in a group.
        TreeSet<Node> inAGroup = new TreeSet<Node>();
        for( String key : nodeMap.keySet() ) {
          if (nodeMap.get(key) instanceof ReactionNode && nodeMap.get(key).row != null) {
            ReactionNode rn = (ReactionNode)nodeMap.get(key);
            if (groups.length() > 0) groups.append(",");
            groups.append("["+nodeToId.get(rn.uniqueName));
            inAGroup.add(rn);
            for( Node n: rn.groupComponents ) {
              groups.append(","+nodeToId.get(n.uniqueName));
              inAGroup.add(n);
            }
            groups.append("]");
          }
        }
        // now add loners
        for( String key : nodeMap.keySet() ) {
          if (nodeMap.get(key).row != null && !inAGroup.contains(nodeMap.get(key))) {
            if (groups.length() > 0) groups.append(",");
            groups.append("["+nodeToId.get(nodeMap.get(key).uniqueName)+"]");
          }
        }

        String json = "{\"nodes\":["+nodes.toString()+"],\"links\":["+links.toString()+
            "],\"groups\":["+groups.toString()+"]}";

        String md5sum = "";
        try {
          MessageDigest md;
          md = MessageDigest.getInstance("MD5");
          BigInteger bI = new BigInteger(1,md.digest(json.getBytes()));
          md5sum = String.format("%1$032x",bI);
        } catch (NoSuchAlgorithmException e) {
          throw new BuildException("No such algorithm!");
        }

        pathway.setAttribute("json",json);
        pathway.setAttribute("json_md5",md5sum);


        LOG.info("Storing pathway "+pathwayName+" ("+pathway.getIdentifier()+") with "+components.size()+" components.");
        // store the pathway
        store(pathway);
        // and the specific instance
        Item oP = createItem("OrganismPathway");
        oP.setReference("pathway",pathway);
        oP.setReference("organism",organism);
        if (method != null) oP.setAttribute("method",method);
        store(oP);
        // and its components
        for(Item component: components) {
          component.setReference("pathway",pathway.getIdentifier());
          LOG.info("\tComponent "+component.getAttribute("identifier")+","+component.getAttribute("step")+","+component.getAttribute("level")+","+component.getReference("pathway"));
          store(component);
        }
      }
    }
  }

  /* getReactionSteps identifies all Biochemical and spontaneous reactions in a pathway
   * and loads them into a map of unique identifier to ReactionNodes.
   * This is just the skeleton of the pathway. We'll need to walk
   * through everything to find the inputs, outputs and substrates.
   * */

  TreeMap<String,Node> getReactionSteps(Pathway pathway,String pathwayID) {

    TreeMap<String,Node> retSet = new TreeMap<String,Node>();
    int reactionCtr = 1;
    // yet another hash. The owl specific name to my (hopefully) universal, unique reaction names.

    TreeMap<String,String> uNameMap = new TreeMap<String,String>();
    for( PathwayStep step: pathway.getPathwayOrder() ) {
      ReactionNode activeNode = new ReactionNode();
      String longName = simplifyKey(step.toString());
      if (uNameMap.containsKey(longName)) {
        activeNode.uniqueName = uNameMap.get(longName);
      } else {
        activeNode.uniqueName = pathwayID +  "_reaction_" + reactionCtr;
        uNameMap.put(longName,activeNode.uniqueName);
        reactionCtr++;
      }
      activeNode.label = activeNode.uniqueName;

      for( org.biopax.paxtools.model.level3.Process proc: step.getStepProcess()) {
        if (proc instanceof BiochemicalReaction) {
          BiochemicalReaction b = (BiochemicalReaction) proc;
          if (activeNode.info() != null) {
            System.err.println("Step "+activeNode.info()+" has multiple reactions!");
          }
          activeNode.info(((Named)proc).getStandardName());
          for( String ec: b.getECNumber() ) {
            activeNode.ec.add(ec);
          }
          for (PhysicalEntity pe: b.getLeft() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.leftComponents.add(peName);
          }
          for (PhysicalEntity pe: b.getRight() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.rightComponents.add(peName);
          }
        } else if (proc instanceof Catalysis) {
          Catalysis cat = (Catalysis) proc;
          for ( Entity e: cat.getParticipant() ) {
            if (e instanceof Named) {
              if ( ((Named)e).getModelInterface().getSimpleName().equalsIgnoreCase("Protein"))  {
                activeNode.proteins.add(e.getStandardName());
              }
            }
          }
        } else if (proc instanceof Transport) {
          Transport b = (Transport) proc;
          if (activeNode.label() != null) {
            System.err.println("Step "+activeNode.label()+" has multiple reactions!");
          }
          activeNode.label(((Named)proc).getStandardName());
          for (PhysicalEntity pe: b.getLeft() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.leftComponents.add(peName);
          }
          for (PhysicalEntity pe: b.getRight() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.rightComponents.add(peName);
          }
        } else {
          System.err.println("Step process is neither reaction nor catalysis. Is "+proc);
        }
      }

      // determine the next reaction step(s)
      for( PathwayStep nS: step.getNextStep() ) {
        activeNode.nextNode.add(simplifyKey(nS.toString()));
      }

      try {
        // and make sure it is oriented consistently.
        if ( ((BiochemicalPathwayStepImpl)step).getStepDirection() == StepDirection.RIGHT_TO_LEFT) {
          // we want to orient everything left-to-right
          Vector<String> t = activeNode.leftComponents;
          activeNode.leftComponents = activeNode.rightComponents;
          activeNode.rightComponents = t;
        }
      } catch (ClassCastException e) {
        throw new BuildException("Cannot cast "+step+" to a BiochemicalPathwayStepImpl.");
      }
      retSet.put(activeNode.uniqueName,activeNode);
    }
    // before we go, relabel all those next steps
    for(String uName : retSet.keySet()) {
      if (retSet.get(uName) instanceof ReactionNode) {
        ReactionNode rN = (ReactionNode)retSet.get(uName);
        TreeSet<String> oldNext = new TreeSet<String>(rN.nextNode);
        rN.nextNode.clear();
        for(String oldLabel : oldNext) {
          rN.nextNode.add(uNameMap.get(oldLabel));
        }
      }
    }

    return retSet;
  }
  private void walk(Integer rowI, Integer columnI,ReactionNode prevNode,ReactionNode currentNode,
      HashSet<Node[]> linkSet,TreeMap<String,Node> nodeMap) {

    // if we've seen both the current and previous nodes, we've walked this section
    // of the graph already. Do no double walk.
    String hopName = ((prevNode==null)?"null":prevNode.uniqueName)+":"+((currentNode==null)?"null":currentNode.uniqueName);
    if (beenThereDoneThat.contains(hopName)) return;
    beenThereDoneThat.add(hopName);

    if ((currentNode != null) && (currentNode.row() == null) ) currentNode.row(rowI);
    if ((currentNode != null) && (currentNode.column() == null) ) currentNode.column(columnI);


    TreeSet<String> linkingComponent = new TreeSet<String>();
    if (prevNode != null && currentNode != null) {
      // we need to determine the product(s) that link the 2 reactions.
      for( String component: currentNode.leftComponents) {
        // we want to make a common node for all components that
        // are in both reactions. But separate ones if the component
        // is only in the left or right.
        if( prevNode!=null && prevNode.rightComponents.contains(component) ) {
          linkingComponent.add(component);
        }
      }
      if (linkingComponent.size() > 0) {
        StringBuffer label = new StringBuffer();
        for( String c: linkingComponent) {
          //TODO: do we want to sort these so that the longest names are first?
          if (label.length() > 0) label.append("<br>");
          label.append(c);
        }
        // try to reuse linking nodes.
        LinkingNode linkN;
        if (nodeMap.containsKey(label.toString()) && nodeMap.get(label.toString()) instanceof LinkingNode) {
          linkN = (LinkingNode) nodeMap.get(label.toString());
        } else {
          linkN = new LinkingNode();
          linkN.uniqueName = label.toString();//prevNode.uniqueName +":"+ currentNode.uniqueName;
          linkN.label(label.toString());
          linkN.row((prevNode.row() + currentNode.row())/2);
          linkN.column((prevNode.column() + currentNode.column())/2);
          nodeMap.put(linkN.uniqueName,linkN);
        }
        // add both links
        Node [] a = new Node[2];
        a[0] = linkN;
        a[1] = currentNode;
        linkSet.add(a);
        a = new Node[2];
        a[0] = prevNode;
        a[1] = linkN;
        linkSet.add(a);
      } else {
        SpontaneousNode sN = new SpontaneousNode();
        sN.uniqueName = prevNode.uniqueName + ":" + currentNode.uniqueName;
        sN.label("spontaneous");
        sN.row((prevNode.row() + currentNode.row())/2);
        sN.column((prevNode.column() + currentNode.column())/2);
        nodeMap.put(sN.uniqueName,sN);
        Node[] a = new Node[2];
        a[0] = sN;
        a[1] = currentNode;
        linkSet.add(a);
        a = new Node[2];
        a[0] = prevNode;
        a[1] = sN;
        linkSet.add(a);
      }
    }

    // now go through and sweep up unused left components of the
    // current node into an input node, and the unused right
    // components of the previous node into an output node.

    if (currentNode != null) {
      StringBuffer label = makeComponentNodeLabel(linkingComponent,currentNode.leftComponents);
      if (label.length() > 0) {
        InputNode inputN;
        // We want a unique name for this node
        // so append the uniquename of the currentNode to this
        String key = label.toString()+":"+currentNode.uniqueName;
        if (!nodeMap.containsKey(key) ) {
          inputN = new InputNode();
          inputN.label(label.toString());
          inputN.uniqueName = key;
          // the input is put on the previous row of the current
          inputN.row(new Integer(currentNode.row() - 1));
          // and one column over. EXCEPT for the first row.
          inputN.column(inputN.row().equals(0)?new Integer(currentNode.column()):new Integer(currentNode.column() + 1));
          nodeMap.put(inputN.uniqueName,inputN);
        }
        inputN = (InputNode) nodeMap.get(key);
        if (!currentNode.groupComponents.contains(inputN) ) currentNode.groupComponents.add(inputN);
        Node[] a = new Node[2];
        a[0] = inputN;
        a[1] = currentNode;
        linkSet.add(a);
      }
    }

    // and repeat for previous node.
    if(prevNode != null) {
      StringBuffer label = makeComponentNodeLabel(linkingComponent,prevNode.rightComponents);
      if (label.length() > 0) {
        // again, make a uniquename out of this (by prepending uniquename of previous Node)
        // and insert if we have to
        String key = prevNode.uniqueName+":"+label.toString();
        OutputNode outputN;
        if (!nodeMap.containsKey(key) ) {
          outputN = new OutputNode();
          outputN.uniqueName = key;
          outputN.label(label.toString());
          // this output is put on the next row of the previous.
          outputN.row(new Integer(prevNode.row() + 1));
          // and one column over.
          outputN.column(new Integer(prevNode.column() + 1));
          nodeMap.put(outputN.uniqueName,outputN);
        }
        outputN = (OutputNode) nodeMap.get(key);
        if (!prevNode.groupComponents.contains(outputN) ) prevNode.groupComponents.add(outputN);
        Node[] a = new Node[2];
        a[0] = prevNode;
        a[1] = outputN;
        linkSet.add(a);
      }
    }

    // and proceed. We click up the ReactionNode row by 4 from the row of the current ReactionNode
    Integer nextrowI = new Integer(rowI+4);
    Integer nextcolumnI = columnI;
    if(currentNode != null ) {
      if (currentNode.nextNode.size()>0) {
        for( String nextNodeLabel: currentNode.nextNode) {
          walk(nextrowI,nextcolumnI,currentNode,(ReactionNode) nodeMap.get(nextNodeLabel),linkSet,nodeMap);
          nextcolumnI = new Integer(maxColumn(nodeMap)+1);
        }
      } else {
        walk(nextrowI,nextcolumnI,currentNode,null,linkSet,nodeMap);
      }
    }
  }

  /*
   * Look for the maximum column (so far) of all nodes.
   */

  int maxColumn(TreeMap<String,Node> nodeMap) {
    int ml = 0;
    for(Node node: nodeMap.values()) {
      if (node.column() != null && node.column().intValue() > ml) {
        ml = node.column().intValue();
      }
    }
    return ml;
  }
  // simplify this key a bit. Remove the http://biocyc.org... part
  private String simplifyKey(String s) {
    if (s.startsWith("http://biocyc.org/biopax/biopax-level3#")) {
      return s.substring("http://biocyc.org/biopax/biopax-level3#".length());
    } else {
      return s;
    }
  }
  private StringBuffer makeComponentNodeLabel(Set<String> exclude,Vector<String> components) {

    TreeMap<String,Integer> componentCtr = new TreeMap<String,Integer>();
    for(String component: components) {
      if (!exclude.contains(component)) {
        if (!componentCtr.containsKey(component)) {
          componentCtr.put(component,new Integer(1));
        } else {
          componentCtr.put(component,new Integer(componentCtr.get(component)+1));
        }
      }
    }
    StringBuffer label = new StringBuffer();
    if (componentCtr.size() > 0) {
      for( String component: componentCtr.keySet()) {
        if (label.length() > 0) label.append("<br>");
        if (componentCtr.get(component) > 1) label.append(componentCtr.get(component)+" "+component);
        else label.append(component);
      }
    }
    return label;
  }
  public void setProteomeId(String proteomeString) throws ObjectStoreException {
    try {
      proteomeId = new Integer(proteomeString);
      organism = createItem("Organism");
      organism.setAttribute("proteomeId",proteomeId.toString());
      store(organism);
    } catch (NumberFormatException e) {
      throw new BuildException("proteome id "+proteomeString+" cannot be parsed as an integer.");
    }
  }
  public void setMethod(String method) {
    this.method=method;
  }

  private class Node implements Comparable {
    public String uniqueName = null;
    protected String label = null;
    protected String info = null;
    private Integer row = null;
    private Integer column = null;
    protected Integer x = null;
    protected Integer y = null;
    protected String linkType = null;

    public String linkType(String s) { return s;}

    public void label(String lab) { label = lab;}
    public void info(String s) { info=s;}
    public String info() { return info; }
    public void row(Integer s) { row = s; }
    public void column(Integer l) { column = l; }
    public Integer row() { return row; }
    public Integer column() { return column;}
    public String label() { return label;}
    public Integer x() { return x; }
    public Integer y() { return y; }
    public void x(Integer xx) { x = xx;}
    public void y(Integer yy) { y = yy;}
    public int compareTo(Object other) {
      if(other instanceof Node) {
        return label.compareTo(((Node)other).label);
      }else {
        return 0;
      }
    }
  }

  private class ReactionNode extends Node {
    public TreeSet<String> proteins = new TreeSet<String>();
    public TreeSet<String> nextNode = new TreeSet<String>();
    public TreeSet<String> prevNode = new TreeSet<String>();
    public boolean isInitial = false;
    public boolean isTerminal = false;
    public TreeSet<String> ec = new TreeSet<String>();
    public boolean isSpontaneous = false;
    public Vector<String> leftComponents = new Vector<String>();
    public Vector<String> rightComponents = new Vector<String>();
    public TreeSet<Node> groupComponents = new TreeSet<Node>();
    public String toString() {
      return "ReactionNode:"+uniqueName+", reaction:"+label+", ec:"+ec+", next: "+nextNode+", proteins:"+proteins+", components:"+leftComponents+","+rightComponents;
    }
  }

  private class InputNode extends Node {
    public String linkType(String s) { return "input";}
    public TreeSet<String> reactions = new TreeSet<String>();
    public String toString() {
      return "InputNode:"+label+" with components "+reactions;
    }
  }
  private class OutputNode extends Node {
    public String linkType(String s) { return "output";}
    public TreeSet<String> reactions = new TreeSet<String>();
    public String toString() {
      return "OutputNode:"+label+" with components "+reactions;
    }
  }
  private class LinkingNode extends Node {
  }
  private class SpontaneousNode extends Node {
  }
}
