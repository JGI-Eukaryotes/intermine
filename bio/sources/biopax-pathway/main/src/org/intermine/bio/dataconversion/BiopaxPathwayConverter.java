package org.intermine.bio.dataconversion;

/*
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biopax.paxtools.impl.level3.BiochemicalPathwayStepImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.ComplexAssembly;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Modulation;
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
import org.intermine.xml.full.ReferenceList;

/** Load the data from owl files into the pathway tables.
 * This is designed to be run in two different modes: once without
 * the organism set in order to make a template for the pathway,
 * and again to capture the proteins for each reaction. Presumably,
 * when run in first mode, the owl file is generated from a organism-agnostic
 * owl file.
 *
 * @author J Carlson
 */
public class BiopaxPathwayConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "Pathologic Pathway";
  private static final String DATA_SOURCE_NAME = "Pathologic";

  private String method = null;
  private Item organism = null;
  private String prefix = null;
  private Item enzymeOntology = null;
  private Item pathwayOntology = null;
  private boolean loadingJSON;
  HashSet<String> beenThereDoneThat = null;
  HashMap<String,Integer> componentCounter = null;
  public enum ReactantType { NOT_SET, LINK, INPUT, OUTPUT }

  // number of pixels between elements in the JSON in the x and y direction.
  private static int pixPerElement = 75;
  private static int pixLabelOffset = 10;

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

    // If the organism is not set, then we are loading the
    // pathwayInfo table. This is the owl file that will be
    // used to load the json for the network.
    if (organism == null || organism.getAttribute("proteomeId") == null ) {
      loadingJSON = true;
    } else if (prefix == null ) {
      throw new BuildException("Organism-specific prefix for pathway name must be set.");
    } else {
      loadingJSON = false;
    }

    SimpleIOHandler handler = new SimpleIOHandler(BioPAXLevel.L3);
    org.biopax.paxtools.model.Model model = handler.convertFromOWL(new ReaderInputStream(reader));
    componentCounter = countComponents(model);
    Set<Pathway> pathwaySet = model.getObjects(Pathway.class);

    // record the ontologies and make a map to store the observed entries
    enzymeOntology = createItem("Ontology");
    enzymeOntology.setAttribute("name","ENZYME");
    store(enzymeOntology);
    pathwayOntology = createItem("Ontology");
    pathwayOntology.setAttribute("name","Pathway");
    store(pathwayOntology);

    // hashes for enzymes and proteins that we register along the way.
    TreeMap<String,Item> enzymeHash = new TreeMap<String,Item>();
    TreeMap<String,Item> proteinHash = new TreeMap<String,Item>();

    for (Pathway pathwayObj : pathwaySet) {

      String pathwayID = null;
      String pathwayName = ((Named)pathwayObj).getStandardName();
      for ( Xref xref: pathwayObj.getXref() ) {
        String db = xref.getDb();
        if (db != null && db.endsWith("Cyc")) {
          pathwayID = xref.getId();
        }
      }

      // certain things are messed up
      if (!pathwayID.equals("PWY-6857") && 
          !pathwayID.equals("PWY-6861") &&
          !pathwayID.equals("PWY-6872") &&
          !pathwayID.equals("PWY-6875")) {
        LOG.info("Processing current pathway "+pathwayObj.toString()+", id: "+pathwayID+" name is "+pathwayName);

        // the first walk of the pathway just gets the reactions
        HashMap<String,Node> nodeMap = getReactionSteps(pathwayObj,pathwayID);

        // we'll need the initial nodes.
        // First, determine the reverse links
        for(String node: nodeMap.keySet()) {
          if ( nodeMap.get(node) instanceof ReactionNode) {
            for( String n: ((ReactionNode)nodeMap.get(node)).nextNode) {
              ((ReactionNode)nodeMap.get(n)).prevNode.add(node);
            }
          }
        }
        // now make a new hash, but use the label instead of
        // the unique name since the unique name only exists in 1 owl file.
        // The idea is that this will give a consistent ordering across owl files.
        TreeMap<String,Node> keyMap = new TreeMap<String,Node>();
        for( String uniquename: nodeMap.keySet()) {
          Node node = nodeMap.get(uniquename);
          if (node==null) {
            throw new BuildException("There is no node stored for "+uniquename);
          }
          if (node.key()==null) {
            throw new BuildException("There is no key entered for "+node.label()+" "+node.uniqueName);
          }
          keyMap.put(node.key(),node);
        }
        // and update the next/prev nodes inside to use the labels.
        for( Node node: nodeMap.values() ) {
          TreeSet<String> next = new TreeSet<String>(((ReactionNode) node).nextNode);
          ((ReactionNode)node).nextNode.clear();
          for(String uniq: next) {
            ((ReactionNode)node).nextNode.add(nodeMap.get(uniq).key());
          }
          TreeSet<String> prev = new TreeSet<String>(((ReactionNode) node).prevNode);
          ((ReactionNode)node).prevNode.clear();
          for(String uniq: prev) {
            ((ReactionNode)node).prevNode.add(nodeMap.get(uniq).key());
          }
        }

        // and use this to find the initial node(s)
        TreeSet<String> initialNodes = new TreeSet<String>();
        TreeSet<String> terminalNodes = new TreeSet<String>();
        for(String node: keyMap.keySet()) {
          if ( keyMap.get(node) instanceof ReactionNode) {
            if (((ReactionNode)keyMap.get(node)).nextNode.size() == 0) {
              terminalNodes.add(node);
            }
            if (((ReactionNode)keyMap.get(node)).prevNode.size() == 0) {
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
        // if a pathway is purely cyclic, we need to pick one more or less
        // at random. But we cannot pick one that is really part of the cycle
        // and is not on some siding.
        // pick one at random, then walk backwards untill we hit something we've
        // already seen.
        if (initialNodes.size() == 0 && keyMap.keySet().size() > 0) {
          // if a pathway is purely cyclic, we need to pick one more or less
          // at random. But we cannot pick one that is really part of the cycle
          // and is not on some siding.
          // pick one at random, then walk backwards untill we hit something we've
          // already seen.
          LOG.warn("Pathway "+pathwayID+" has no initial nodes. Picking...");
          LinkedList<String> toBeExamined = new LinkedList<String>();
          toBeExamined.add((String) keyMap.keySet().toArray()[0]);
          HashSet<String> seen = new HashSet<String>();
          while(initialNodes.size() == 0 && toBeExamined.size() > 0) {
            String node = toBeExamined.removeFirst();
            if (seen.contains(node)) {
              // we've doubled back on this. We're done
              LOG.warn("Pathway "+pathwayID+" has no initial nodes. Picking "+keyMap.keySet().toArray()[0]);
              initialNodes.add(node);
            } else {
              seen.add(node);
              for(String n: ((ReactionNode)keyMap.get(node)).prevNode) {
                toBeExamined.add(n);
              }
            }
          }
        }
        if(initialNodes.size() > 0) {
          // we need to track node pairs we've visited to avoid double tracking
          beenThereDoneThat = new HashSet<String>();

          // as we walk, we'll assign column and row integers
          // to the reaction nodes, inputs and output.
          // the 'side' inputs and outputs will be at column-1 and
          // column+1 compared to the reaction (and row+1). The linking
          // input will be at column-2, output will be at column+2 (and the
          // same row
          // when we start the walk the first reaction
          // will be set this to 2 (rather than 0) to have a
          // column for the inputs.
          Integer columnI = new Integer(2);
          // and this will be the row.
          Integer rowI = new Integer(0);
          // TODO: look for problems with this algorithm. If there are
          // multiple branches in a walk, the second will cross the row
          // created by the first. This is a problem.
          //HashMap<Integer,LinkingNode> linkNodeMap = new HashMap<Integer,LinkingNode>();
          for( String initial: initialNodes) {
            ReactionNode prevNode = null;
            ReactionNode currentNode = (ReactionNode) keyMap.get(initial);
            // this walk is recursive
            walkLinkingNodes(rowI,columnI,(ReactionNode)prevNode,(ReactionNode)currentNode,linkSet,keyMap);
            rowI = new Integer(maxRow(keyMap) + 2);
          }

          beenThereDoneThat.clear();
          for( String initial: initialNodes) {
            ReactionNode prevNode = null;
            ReactionNode currentNode = (ReactionNode) keyMap.get(initial);
            // this walk is recursive
            addIONodes((ReactionNode)prevNode,(ReactionNode)currentNode,linkSet,keyMap);
          }

          storePathway(pathwayID,pathwayName,keyMap,proteinHash,enzymeHash,linkSet);

        } else {
          LOG.warn("Pathway "+pathwayID+" has no initial nodes and cannot find one.");
        }
      }
    }
  }

  /**
   * countComponents
   * scan the owl file and see how often each small molecule is part of a reaction
   * these counts are used when looking for the intermediate products of reactions (since
   * it is sometimes ambiguous) or the initial/final products of a pathway.
   * things that are in more reactions will be considered the side input or output;
   * things that are less common are the intermediate or terminal products
   * @param model
   * @return a hash of small molecule and count.
   */
  private HashMap<String,Integer> countComponents(org.biopax.paxtools.model.Model model) {
    HashMap<String,Integer> retHash = new HashMap<String,Integer>();
    Set<BiochemicalReaction> rSet = model.getObjects(BiochemicalReaction.class);
    for (BiochemicalReaction r: rSet) {

      for (PhysicalEntity pe: r.getLeft() ) {
        String n = ((Named)pe).getStandardName();
        if(!retHash.containsKey(n) ) {
          retHash.put(n,new Integer(0));
        }
        retHash.put(n,retHash.get(n).intValue()+1);
      }
      for (PhysicalEntity pe: r.getRight() ) {
        String n = ((Named)pe).getStandardName();
        if(!retHash.containsKey(n) ) {
          retHash.put(n,new Integer(0));
        }
        retHash.put(n,retHash.get(n).intValue()+1);
      }
    }
    return retHash;
  }

  /**
   * getReactionSteps
   * identifies all Biochemical and spontaneous reactions in a pathway
   * and loads them into a map of unique identifier to ReactionNodes.
   * This is just the skeleton of the pathway. We'll need to walk
   * through everything to find the inputs, outputs and substrates.
   * @param pathway object
   * @param pathwayId the 
   */

  HashMap<String,Node> getReactionSteps(Pathway pathway,String pathwayID) {

    // this is what we will return. A hash indexed by owl file-specific unique id's
    HashMap<String,Node> retSet = new HashMap<String,Node>();
    // some reactions have no MetaCyc labels. These do not appear in organism
    // owl files, just metacyc. Or so it appears. We'll use a temp generated
    // name for these
    Integer nullLabelCtr = new Integer(1);
    for( PathwayStep step: pathway.getPathwayOrder() ) {
      ReactionNode activeNode = new ReactionNode();
      activeNode.uniqueName = simplifyKey(step.toString());
      for( org.biopax.paxtools.model.level3.Process proc: step.getStepProcess()) {
        if (proc instanceof BiochemicalReaction) {
          BiochemicalReaction b = (BiochemicalReaction) proc;
          // was a PAXTools or pathway-tools bug. spontaneous nodes never
          // return 'false'. They were returning 'L-R' Take any non-null to be true.
          if (b.getSpontaneous() != null  && !b.getSpontaneous() ) {
            activeNode.isSpontaneous = true;
            // use this as a label in case nothing else comes up
            activeNode.label("<em>spontaneois</em>");
          }
          for ( Xref xref: b.getXref() ) {
            if (xref.getDb() != null && xref.getDb().endsWith("Cyc")) {
              activeNode.label(xref.getId());
              activeNode.key(Integer.toString(activeNode.label().hashCode()));
            }
          }
          if (activeNode.label() == null) {
            activeNode.label("UnnamedReaction_"+nullLabelCtr.toString());
            activeNode.key(Integer.toString(activeNode.label().hashCode()));
            nullLabelCtr = nullLabelCtr + 1;
          }

          if (activeNode.info() != null) {
            throw new BuildException("Step "+activeNode.info()+" has multiple reactions!");
          }

          LOG.info("ReactionStandardName is "+((Named)proc).getStandardName());
          String ll = ((Named)proc).getStandardName().replaceAll("\"","&quot;");
          activeNode.info(ll);

          for( String ec: b.getECNumber() ) {
            activeNode.ecs().add(ec);
          }
          for (PhysicalEntity pe: b.getLeft() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.leftComponents.put(peName,ReactantType.NOT_SET);
          }
          for (PhysicalEntity pe: b.getRight() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.rightComponents.put(peName,ReactantType.NOT_SET);
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
          for ( Entity e: cat.getController() ) {
            if (e instanceof Named) {
              if ( ((Named)e).getModelInterface().getSimpleName().equalsIgnoreCase("Protein"))  {
                activeNode.proteins.add(e.getStandardName());
              }
            }
          }
        } else if (proc instanceof Transport) {
          Transport b = (Transport) proc;
          if (activeNode.label() != null) {
            throw new BuildException("Step "+activeNode.label()+" has multiple reactions!");
          }
          activeNode.label(((Named)proc).getStandardName());
          activeNode.key(Integer.toString(activeNode.label().hashCode()));
          for (PhysicalEntity pe: b.getLeft() ) {
            String peName = ((Named)pe).getStandardName(); 
            activeNode.leftComponents.put(peName,ReactantType.NOT_SET);
          }
          for (PhysicalEntity pe: b.getRight() ) {
            String peName = ((Named)pe).getStandardName();
            activeNode.rightComponents.put(peName,ReactantType.NOT_SET);
          }
        } else if ( proc instanceof Modulation) {
          // do not process.
        } else if ( proc instanceof ComplexAssembly) {
          // do not process.
        } else {
          System.out.println("Unexpected process: "+proc);
        }
      }
      // sanity
      if (activeNode.isSpontaneous && (activeNode.proteins.size() > 0 || activeNode.ecs.size() > 0)) {
        LOG.warn("A spontaneous node in "+pathwayID+" has proteins or ecs.");
      }

      // determine the next reaction step(s)
      for( PathwayStep nS: step.getNextStep() ) {
        activeNode.nextNode.add(simplifyKey(nS.toString()));
      }

      try {
        // and make sure it is oriented consistently.
        if ( ((BiochemicalPathwayStepImpl)step).getStepDirection() == StepDirection.RIGHT_TO_LEFT) {
          // we want to orient everything left-to-right
          TreeMap<String,ReactantType> t = new TreeMap<String,ReactantType>(activeNode.leftComponents);
          activeNode.leftComponents = activeNode.rightComponents;
          activeNode.rightComponents = t;
        }
      } catch (ClassCastException e) {
        throw new BuildException("Cannot cast "+step+" to a BiochemicalPathwayStepImpl.");
      }

      // store only processed ones.
      if (activeNode.key() != null) retSet.put(activeNode.uniqueName,activeNode);
    }

    return retSet;
  }

  private void walkLinkingNodes(Integer rowI,Integer columnI,ReactionNode prevNode,ReactionNode currentNode,
      HashSet<Node[]> linkSet,TreeMap<String,Node> keyMap) {

    // if we've seen both the current and previous nodes, we've walked this section
    // of the graph already. Do no double walk.
    String hopName = ((prevNode==null)?"null":prevNode.uniqueName)+":"+((currentNode==null)?"null":currentNode.uniqueName);
    if (beenThereDoneThat.contains(hopName)) return;
    beenThereDoneThat.add(hopName);

    if ((currentNode != null) && (currentNode.x() == null) ) currentNode.x(columnI);
    if ((currentNode != null) && (currentNode.y() == null) ) currentNode.y(rowI);


    TreeSet<String> linkingComponent = new TreeSet<String>();
    if (prevNode != null && currentNode != null) {
      // we need to determine the product(s) that link the 2 reactions.
      for( String component: currentNode.leftComponents.keySet()) {
        // we want to make a common node for all components that
        // are in both reactions. But separate ones if the component
        // is only in the left or right.
        if(prevNode.rightComponents.containsKey(component) ) {
          linkingComponent.add(component);
        }
      }
      // if there are multiple linking components, only use the one with the
      // lowest counter.
      if (linkingComponent.size() > 0) {
        String label = null;
        Integer lowestCount = null;
        for(String comp: linkingComponent) {
          // use the less-frequently mention item. Or the longer name if a tie.
          if (label==null || componentCounter.get(comp).intValue() < lowestCount.intValue() ||
              (componentCounter.get(comp).intValue() == lowestCount.intValue() && comp.length() > label.length() )) {
            label = comp;
            lowestCount = componentCounter.get(comp);
          }
        }
        // is it unique?
        for(String comp: linkingComponent) {
          if (!comp.equals(label) && componentCounter.get(comp).equals(componentCounter.get(label))) {
            LOG.warn("There is a tie in the lowest component counter linking component between "+label+" and "+comp);
          }
        }
        linkingComponent.clear();
        linkingComponent.add(label);
        // be sure to mark the label so it does not
        // appear as another input or output later.
        currentNode.leftComponents.put(label,ReactantType.LINK);
        prevNode.rightComponents.put(label,ReactantType.LINK);
        // try to reuse linking nodes.
        // if we find that this link has been used before, either leading into the current node
        // or out of the previous node, we'll use that node
        LinkingNode linkN;

        String primaryKey = Integer.toString(new String(prevNode.label() + ":" + currentNode.label()).hashCode());
        String key1 = Integer.toString(new String(label + ":" + currentNode.label()).hashCode());
        String key2 = Integer.toString(new String(prevNode.label() + ":" + label).hashCode());
        if (keyMap.containsKey(key1) ) {
          try {
            linkN = (LinkingNode)keyMap.get(key1);
          } catch (ClassCastException e) {
            throw new BuildException("The key "+key2+" has the same label as a non-linking node.");
          }
        } else if (keyMap.containsKey(key2) ) {
          try {
            linkN = (LinkingNode)keyMap.get(key2);
          } catch (ClassCastException e) {
            throw new BuildException("The key "+key2+" has the same label as a non-linking node.");
          }
        
        } else {
          linkN = new LinkingNode();;
          linkN.label(label);
          linkN.key(primaryKey);
          linkN.y((prevNode.y() + currentNode.y())/2);
          linkN.x((prevNode.x() + currentNode.x())/2);
          keyMap.put(primaryKey,linkN);
          keyMap.put(key1,linkN);
          keyMap.put(key2,linkN);
        }
        // add both links
        // From the link node to the current node.
        Node [] a = new Node[2];
        a[0] = linkN;
        a[1] = currentNode;
        linkSet.add(a);
        // and from the previous node to the link node.
        a = new Node[2];
        a[0] = prevNode;
        a[1] = linkN;
        linkSet.add(a);
      } else {
        // if we cannot find a good match, try a fuzzy match
        FuzzyScore fS = new FuzzyScore(Locale.ENGLISH);
        Integer bestScore = new Integer(0);
        String bestLeft = null;
        String bestRight = null;
        for( String lComponent: currentNode.leftComponents.keySet()) {
          for( String rComponent: prevNode.rightComponents.keySet()) {
            Integer thisScore = fS.fuzzyScore(lComponent,rComponent);
            LOG.info("FuzzyScore of "+lComponent+" and "+rComponent+" is "+thisScore);
            if (thisScore > bestScore) {
              bestLeft = lComponent;
              bestRight = rComponent;
              bestScore = thisScore;
            }
          }
        }
        if (bestLeft != null) {
          LOG.info("Best FuzzyPair is "+bestLeft+" and "+bestRight);
          currentNode.leftComponents.put(bestLeft,ReactantType.LINK);
          prevNode.rightComponents.put(bestRight,ReactantType.LINK);
          LinkingNode linkN;
          String primaryKey = Integer.toString(new String(prevNode.label() + ":" + currentNode.label()).hashCode());
          String key1 = Integer.toString(new String(bestRight + ":" + currentNode.label()).hashCode());
          String key2 = Integer.toString(new String(prevNode.label() + ":" + bestLeft).hashCode());
          if (keyMap.containsKey(key1) ) {
            try {
              linkN = (LinkingNode)keyMap.get(key1);
            } catch (ClassCastException e) {
              throw new BuildException("The key "+key2+" has the same label as a non-linking node.");
            }
          } else if (keyMap.containsKey(key2) ) {
            try {
              linkN = (LinkingNode)keyMap.get(key2);
            } catch (ClassCastException e) {
              throw new BuildException("The key "+key2+" has the same label as a non-linking node.");
            }
          
          } else {
            linkN = new LinkingNode();;
            linkN.label(bestRight+"/"+bestLeft);
            linkN.key(primaryKey);
            linkN.y((prevNode.y() + currentNode.y())/2);
            linkN.x((prevNode.x() + currentNode.x())/2);
            keyMap.put(primaryKey,linkN);
            keyMap.put(key1,linkN);
            keyMap.put(key2,linkN);
          }
          // add both links
          // From the link node to the current node.
          Node [] a = new Node[2];
          a[0] = linkN;
          a[1] = currentNode;
          linkSet.add(a);
          // and from the previous node to the link node.
          a = new Node[2];
          a[0] = prevNode;
          a[1] = linkN;
          linkSet.add(a);
        }
        
        /*SpontaneousNode sN = new SpontaneousNode();
        sN.key(Integer.toString(new String(prevNode.label() + ":" + currentNode.label()).hashCode()));
        sN.label("<i>spontaneous</i>");
        sN.x((prevNode.x() + currentNode.x())/2);
        sN.y((prevNode.y() + currentNode.y())/2);
        keyMap.put(sN.key(),sN);
        Node[] a = new Node[2];
        a[0] = sN;
        a[1] = currentNode;
        linkSet.add(a);
        a = new Node[2];
        a[0] = prevNode;
        a[1] = sN;
        linkSet.add(a);*/
      }
    } else if ( prevNode == null && currentNode != null) {
      // take the component from the left with the lowest count and
      // call it the source. It is the initial molecule of the reaction
      if( currentNode.leftComponents.size() > 0) {
        String useThisOne = null;
        Integer lowestCount = null;
        for(String comp: currentNode.leftComponents.keySet()) {
          if (useThisOne==null || componentCounter.get(comp).intValue() < lowestCount.intValue() ||
              (componentCounter.get(comp).intValue() == lowestCount.intValue() && comp.length() > useThisOne.length() )) {
            useThisOne = comp;
            lowestCount = componentCounter.get(comp);
          }
        }
        // mark is as used.
        currentNode.leftComponents.put(useThisOne,ReactantType.LINK);
        linkingComponent.clear();
        linkingComponent.add(useThisOne);
        // the key for the input molecule is start:<molecule name>
        String key = Integer.toString(new String("start:"+useThisOne).hashCode());
        SourceNode sourceN = null;
        if (keyMap.containsKey(key)) {
          try {
            sourceN = (SourceNode)keyMap.get(key);
          } catch (ClassCastException e) {
            throw new BuildException("The label "+useThisOne+" has the same hashcode as a non-linking node.");
          }
        } else {
          sourceN = new SourceNode();
          sourceN.label(useThisOne);
          sourceN.key(key);
          sourceN.y(currentNode.y());
          sourceN.x(-1+currentNode.x());
          keyMap.put(sourceN.key(),sourceN);
        }
        Node[] a = new Node[2];
        a[0] = sourceN;
        a[1] = currentNode;
        linkSet.add(a);
      }
    } else if (prevNode != null && currentNode == null) {
      // repeat for the last node
      // call it the link
      if( prevNode.rightComponents.size() > 0) {
        String useThisOne = null;
        Integer lowestCount = null;
        for(String comp: prevNode.rightComponents.keySet()) {
          if (useThisOne==null || componentCounter.get(comp).intValue() < lowestCount.intValue() ||
              (componentCounter.get(comp).intValue() == lowestCount.intValue() && comp.length() > useThisOne.length() )) {
            useThisOne = comp;
            lowestCount = componentCounter.get(comp);
          }
        }
        prevNode.rightComponents.put(useThisOne,ReactantType.LINK);
        linkingComponent.clear();
        linkingComponent.add(useThisOne);
        // but for the key, take ALL components. There seems to be inconsistencies
        // in which is the "starting" components for different organism
        String key = Integer.toString(new String(useThisOne+":end").hashCode());
        DrainNode drainN = null;
        if (keyMap.containsKey(key)) {
          try {
            drainN = (DrainNode)keyMap.get(key);
          } catch (ClassCastException e) {
            throw new BuildException("The label "+useThisOne+" has the same key as a non-linking node.");
          }
        } else {
          drainN = new DrainNode();
          drainN.label(useThisOne);
          drainN.key(key);
          drainN.y(prevNode.y());
          drainN.x(+1 + prevNode.x());
          keyMap.put(drainN.key(),drainN);
        }
        Node[] a = new Node[2];
        a[0] = prevNode;
        a[1] = drainN;
        linkSet.add(a);
      }
    }

    // and proceed. We click up the ReactionNode column by 4 from the column of the current ReactionNode
    Integer nextcolumnI = new Integer(columnI+4);
    Integer nextrowI = rowI;
    if(currentNode != null ) {
      if (currentNode.nextNode.size()>0) {
        for( String nextNodeLabel: currentNode.nextNode) {
          walkLinkingNodes(nextrowI,nextcolumnI,currentNode,(ReactionNode) keyMap.get(nextNodeLabel),linkSet,keyMap);
          nextrowI = new Integer(maxRow(keyMap)+2);
        }
      } else {
        walkLinkingNodes(nextrowI,nextcolumnI,currentNode,null,linkSet,keyMap);
      }
    }
  }

  private void addIONodes(ReactionNode prevNode,ReactionNode currentNode,
      HashSet<Node[]> linkSet,TreeMap<String,Node> nodeMap) {

    // if we've seen both the current and previous nodes, we've walked this section
    // of the graph already. Do no double walk.
    String hopName = ((prevNode==null)?"null":prevNode.uniqueName)+":"+
        ((currentNode==null)?"null":currentNode.uniqueName);
    if (beenThereDoneThat.contains(hopName)) return;
    beenThereDoneThat.add(hopName);

    // now go through and sweep up unused left components of the
    // current node into an input node, and the unused right
    // components of the previous node into an output node.
    // but pay special attention to the first and last nodes.

    if (currentNode != null) {
      String label = makeComponentNodeLabel(currentNode.leftComponents,false);
      if (label.length() > 0) {
        InputNode inputN;
        // We want a unique name for this node
        // so append the uniquename of the currentNode to this
        String key = Integer.toString(new String(label.toString()+":"+currentNode.label()).hashCode());
        LOG.info("input nodekey is from string "+label.toString()+":"+currentNode.key+" and is "+key.toString());
        if (!nodeMap.containsKey(key) ) {
          inputN = new InputNode();
          inputN.label(label.toString());
          inputN.key(key);
          // the input is put on the previous column of the current
          inputN.x(new Integer(currentNode.x() - 1));
          // and one row over. EXCEPT for the first column.
          inputN.y(new Integer(currentNode.y() + 1));
          nodeMap.put(inputN.key(),inputN);
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
      String label = makeComponentNodeLabel(prevNode.rightComponents,false);
      if (label.length() > 0) {
        // again, make a uniquename out of this (by prepending uniquename of previous Node)
        // and insert if we have to
        String key = Integer.toString(new String(prevNode.label+":"+label.toString()).hashCode());
        LOG.info("output nodekey is from string "+prevNode.key+":"+label.toString()+" and is "+key.toString());
        OutputNode outputN;
        if (!nodeMap.containsKey(key) ) {
          outputN = new OutputNode();
          outputN.label(label.toString());
          outputN.key(key);
          // this output is put on the next column of the previous.
          outputN.x(new Integer(prevNode.x() + 1));
          // and one row over. EXCEPT for the last node.
          //outputN.row(iN.column().equals(0)?new Integer(currentNode.row()):new Integer(currentNode.row() + 1));
          outputN.y(new Integer(prevNode.y() + 1));
          nodeMap.put(outputN.key(),outputN);
        }
        outputN = (OutputNode) nodeMap.get(key);
        if (!prevNode.groupComponents.contains(outputN) ) prevNode.groupComponents.add(outputN);
        Node[] a = new Node[2];
        a[0] = prevNode;
        a[1] = outputN;
        linkSet.add(a);
      }
    }
    // and walk
    if(currentNode != null ) {
      if (currentNode.nextNode.size()>0) {
        for( String nextNodeLabel: currentNode.nextNode) {
          addIONodes(currentNode,(ReactionNode) nodeMap.get(nextNodeLabel),linkSet,nodeMap);
        }
      } else {
        addIONodes(currentNode,null,linkSet,nodeMap);
      }
    }

  }

  void storePathway(String pathwayID,String pathwayName, TreeMap<String, Node> keyMap, TreeMap<String, Item> proteinHash,
      TreeMap<String, Item> enzymeHash, HashSet<Node[]> linkSet) throws ObjectStoreException {
    // Time to store
    // first the components. If we're loading the JSON, we want to
    // populate the entire record. But if it's only for the organism,
    // we just want the key.

    Item pathwayInfo = createItem("PathwayInfo");
    if (pathwayID != null) pathwayInfo.setAttribute("identifier", pathwayID);
    if (pathwayName != null) pathwayInfo.setAttribute("name", pathwayName);

    TreeSet<Item> components = new TreeSet<Item>();
    for(String key: keyMap.keySet() ) {
      Node activeNode = keyMap.get(key);
     
      Item component = createItem("PathwayComponent");
      component.setAttribute("key",activeNode.key());
      if ( activeNode.x() != null) component.setAttribute("level",new Integer(activeNode.x()).toString());
      if ( activeNode.y() != null) component.setAttribute("step",new Integer(activeNode.y()).toString());
      if (activeNode instanceof InputNode) {
        component.setAttribute("type","input");
      } else if (activeNode instanceof OutputNode) {
        component.setAttribute("type","output");
      } else if (activeNode instanceof LinkingNode) {
        component.setAttribute("type","link");
      } else if (activeNode instanceof SourceNode) {
        component.setAttribute("type","source");
      } else if (activeNode instanceof DrainNode) {
        component.setAttribute("type","drain");
      } else if (activeNode instanceof ReactionNode) {
        component.setAttribute("type","reaction");
        if (!loadingJSON) {
          for( String protein: ((ReactionNode)activeNode).proteins ) {
            if (!proteinHash.containsKey(protein) ) {
              Item p = createItem("Protein");
              p.setAttribute("primaryIdentifier",protein);
              p.setReference("organism",organism);
              store(p);
              proteinHash.put(protein,p);
            }
            component.addToCollection("proteins",proteinHash.get(protein));
            // TODO: add pathway ontology term to protein.
          }
        }
        for( String ec: ((ReactionNode)activeNode).ecs ) {
          if (!enzymeHash.containsKey(ec) ) {
            Item e = createItem("OntologyTerm");
            e.setAttribute("identifier",ec);
            e.setReference("ontology",enzymeOntology);
            store(e);
            enzymeHash.put(ec,e);
          }
          component.addToCollection("ontologyTerms",enzymeHash.get(ec));
        }
      } else if (activeNode instanceof SpontaneousNode) {
        component.setAttribute("type","link");
      } else {
        throw new BuildException("Unknown node type. Is this a node? "+key+" "+keyMap.get(key));
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


    for( String key : keyMap.keySet()) {
      Node activeNode = keyMap.get(key);
      if (key.equals(activeNode.key)) {
        if( activeNode.x() != null && activeNode.y() != null) {
          nodeToId.put(key,currentId);
          if (nodes.length() > 0 ) nodes.append(",");
          nodes.append(activeNode.toJSON(currentId));
          currentId = currentId + 1;
        } else {
          LOG.warn("There are untraveled reactions in "+pathwayID);
        }
      }
    }

    for( Node[] pair: linkSet) {

      if (links.length() > 0) links.append(",");
      links.append("{\"source\":"+nodeToId.get(pair[0].key()) +
          ",\"target\":"+nodeToId.get(pair[1].key())+",\"type\":\""+
          (pair[0].nodeType().equals("input")?"input":pair[1].nodeType().equals("output")?"output":"link")+
          "\"}");

    }

    // keep track of who is in a group. We'll construct groups
    // of single elements for those not otherwise in a group.
    TreeSet<Node> inAGroup = new TreeSet<Node>();
    for( String key : keyMap.keySet() ) {
      if (key.equals(keyMap.get(key).key)) {
      if (keyMap.get(key) instanceof ReactionNode && keyMap.get(key).y != null) {
        ReactionNode rn = (ReactionNode)keyMap.get(key);
        if (groups.length() > 0) groups.append(",");
        groups.append("["+nodeToId.get(rn.key));
        inAGroup.add(rn);
        for( Node n: rn.groupComponents ) {
          groups.append(","+nodeToId.get(n.key));
          inAGroup.add(n);
        }
        groups.append("]");
      }
      }
    }
    // now add loners
    for( String key : keyMap.keySet() ) {
      if (key.equals(keyMap.get(key).key)) {
      if (keyMap.get(key).y != null && !inAGroup.contains(keyMap.get(key))) {
        if (groups.length() > 0) groups.append(",");
        groups.append("["+nodeToId.get(keyMap.get(key).key)+"]");
      }
      }
    }

    String json = "{\"nodes\":["+nodes.toString()+"],\"links\":["+links.toString()+
        "],\"groups\":["+groups.toString()+"]}";

    // store the pathwayinfo and it's JSON
    store(pathwayInfo);
    if (loadingJSON) {
      Item pJSON = createItem("PathwayJSON");
      pJSON.setAttribute("json",json);
      pJSON.setReference("pathwayInfo",pathwayInfo);
      store(pJSON);
    }

    if (!loadingJSON) {
      // and the specific instance
      Item pathway = createItem("Pathway");
      pathway.setAttribute("identifier",prefix+
          " "+pathwayInfo.getAttribute("identifier").getValue());
      pathway.setReference("pathwayInfo",pathwayInfo);
      pathway.setReference("organism",organism);
      ReferenceList allProts = new ReferenceList("proteins");
      // look at all components and add the proteins for each
      // one at a time. This will prevent redundancies
      for( Item component: components) {
        ReferenceList prots = component.getCollection("proteins");
        if (prots != null) {
          for(String refId: prots.getRefIds()) {
            allProts.addRefId(refId);
          }
        }
      }
      pathway.addCollection(allProts);
      if (method != null) pathway.setAttribute("method",method);
      store(pathway);
    }

    // and the components
    for(Item component: components) {
      component.setReference("pathwayInfo",pathwayInfo.getIdentifier());
      // if we're nt loading the JSON, we only need to touch the reactions
      // to get the proteins and ECs in.
      if(loadingJSON || component.getAttribute("type").getValue().equals("reaction")) {
        store(component);
      }
    }
  }
  String cleanUp(String a) {
    return a.replaceAll("\"","&quot;");
  }

  /*
   * Look for the maximum y (so far) of all nodes.
   */

  int maxRow(TreeMap<String,Node> keyMap) {
    int ml = 0;
    for(Node node: keyMap.values()) {
      if (node.y() != null && node.y().intValue() > ml) {
        ml = node.y().intValue();
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
  private String makeComponentNodeLabel(TreeMap<String,ReactantType> components,boolean useAll) {

    TreeMap<String,Integer> componentCtr = new TreeMap<String,Integer>();
    for(String component: components.keySet()) {
      if (useAll || components.get(component) == ReactantType.NOT_SET) {
        if (!componentCtr.containsKey(component)) {
          componentCtr.put(component,new Integer(1));
        } else {
          componentCtr.put(component,new Integer(componentCtr.get(component)+1));
        }
      }
    }
    StringBuffer label = new StringBuffer();
    if (componentCtr.size() > 0) {
      // if (componentCtr.size() > 1 ) System.out.println("sorted keys? "+componentCtr.keySet());
      //TreeSet<String> ss = new TreeSet<String>(componentCtr.keySet());
      for( String component: componentCtr.keySet()) {
        if (label.length() > 0) label.append("<br>");
        if (componentCtr.get(component) > 1) label.append(componentCtr.get(component).toString()+" "+component);
        else label.append(component);
      }
    }
    return label.toString().replaceAll("\"","&quot;");
  }

  public void setOrganismPrefix(String prefix) {
    this.prefix = prefix;
  }
  public String getOrganismPrefix() {
    return prefix;
  }

  public void setProteomeId(String proteomeId) throws ObjectStoreException {
    if (organism == null) {
      organism = createItem("Organism");
    }
    try {
      organism.setAttribute("proteomeId",new Integer(proteomeId).toString());
    } catch (NumberFormatException e) {
      throw new ObjectStoreException("proteomeId is not an integer.");
    }
    store(organism);
  }

  public void setMethod(String method) {
    this.method=method;
  }

  private class Node implements Comparable {
    protected String uniqueName = null;
    protected String label = null;
    // the hashcode key, stored as a string
    protected String key = null;
    protected String info = null;
    protected Integer x = null;
    protected Integer y = null;
    protected String nodeType = "unknown";

    public String nodeType() { return nodeType;}

    public void label(String label) { this.label = new String(label); }
    public void uniqueName(String uniqueName) { this.uniqueName= new String(uniqueName); }
    public void key(String key) { this.key=new String(key); }
    public void key(Integer key) { this.key=key.toString();}
    public void info(String info) { this.info=info;}
    public void x(Integer x) { this.x = x;}
    public void y(Integer y) { this.y = y;}
    public String key() { return key;}
    public String info() { return info; }
    public String label() { return label;}
    public Integer x() { return x;}
    public Integer y() { return y;}
    public int compareTo(Object other) {
      if(other instanceof Node) {
        return key.compareTo(((Node)other).key);
      }else {
        return 0;
      }
    }
    public String toJSON(Integer id) {
      return "{\"id\":"+id.toString() +
          (label==null?"":",\"label\":\""+cleanUp(label)+"\",\"labelX\":"+(x()*pixPerElement)+",\"labelY\":"+(y()*pixPerElement-pixLabelOffset)) +
          ",\"x\":"+x()*pixPerElement +
          ",\"y\":"+y()*pixPerElement +
          ",\"orient\":\"0\""+
          ",\"type\":\""+nodeType+"\""+
          ",\"key\":"+key+
          "}" ;
    }
  }

  private class ReactionNode extends Node {
    protected String nodeType =  "reaction";
    // these are literal protein or ec identifiers.
    public TreeSet<String> proteins = new TreeSet<String>();
    public TreeSet<String> ecs = new TreeSet<String>();
    // these are keys into a hash of nodes
    public TreeSet<String> nextNode = new TreeSet<String>();
    public TreeSet<String> prevNode = new TreeSet<String>();
    public boolean isInitial = false;
    public boolean isTerminal = false;
    public boolean isSpontaneous = false;
    // molecules going into or out of a reaction. keyed by the molecule. And
    // labeled as to whether it's an input, output, link or not set.
    public TreeMap<String,ReactantType> leftComponents = new TreeMap<String,ReactantType>();
    public TreeMap<String,ReactantType> rightComponents = new TreeMap<String,ReactantType>();
    public TreeSet<Node> groupComponents = new TreeSet<Node>();
    public TreeSet<String> ecs() { return ecs; }
    public TreeSet<String> proteins() { return proteins; }
    public String toString() {
      return "ReactionNode:"+key+", label:"+label+", ec:"+ecs+", next: "+nextNode+", proteins:"+proteins+", components:"+leftComponents+","+rightComponents;
    }
    @Override
    public String toJSON(Integer id) {
      return "{\"id\":"+id.toString() +
          (label==null?"":",\"label\":\""+cleanUp(label)+"\",\"labelX\":"+(x()*pixPerElement)+",\"labelY\":"+(y()*pixPerElement-2*pixLabelOffset)) +
          ",\"tooltip\":\""+info()+"\""+
          ",\"x\":"+x()*pixPerElement +
          ",\"y\":"+y()*pixPerElement +
          ",\"orient\":\"0\""+
          ",\"type\":\"reaction\""+
          ",\"key\":"+key +
          "}";
    }
  }
  
  private class IONode extends Node {
    public TreeSet<String> reactions = new TreeSet<String>();
    // for the input/output nodes, default label is below
    @Override
    public String toJSON(Integer id) {
      return "{\"id\":"+id.toString() +
          (label==null?"":",\"label\":\""+cleanUp(label)+"\",\"labelX\":"+(x()*pixPerElement)+",\"labelY\":"+(y()*pixPerElement+pixLabelOffset)) +
          ",\"x\":"+x()*pixPerElement +
          ",\"y\":"+y()*pixPerElement +
          ",\"orient\":\"0\""+
          ",\"type\":\""+nodeType+"\""+
          ",\"key\":"+key +
          "}";
    }
  }
  private class InputNode extends IONode {
    public TreeSet<String> reactions = new TreeSet<String>();
    public InputNode() {
      nodeType = "input";
    }
    public String toString() {
      return "InputNode:"+label+" with components "+reactions;
    }
  }
  private class OutputNode extends IONode {
    public TreeSet<String> reactions = new TreeSet<String>();
    public OutputNode() {
      nodeType = "output";
    }
    public String toString() {
      return "OutputNode:"+label+" with components "+reactions;
    }
  }
  private class LinkingNode extends Node {
    public LinkingNode() {
      nodeType = "link";
    }
  }

  private class SourceNode extends Node {
    public SourceNode() {
      nodeType = "source";
    }
    @Override
    public String toJSON(Integer id) {
      return "{\"id\":"+id.toString() +
          (label==null?"":",\"label\":\""+cleanUp(label)+"\",\"labelX\":"+(x()*pixPerElement-pixLabelOffset)+",\"labelY\":"+(y()*pixPerElement-2*pixLabelOffset)) +
          ",\"x\":"+x()*pixPerElement +
          ",\"y\":"+y()*pixPerElement +
          ",\"orient\":\"0\""+
          ",\"type\":\"source\""+
          ",\"key\":"+key +
          "}";
    }
  }
  private class DrainNode extends Node {
    public DrainNode() {
      nodeType = "drain";
    }
    @Override
    public String toJSON(Integer id) {
      return "{\"id\":"+id.toString() +
          (label==null?"":",\"label\":\""+cleanUp(label)+"\",\"labelX\":"+(x()*pixPerElement+pixLabelOffset)+",\"labelY\":"+(y()*pixPerElement-2*pixLabelOffset)) +
          ",\"x\":"+x()*pixPerElement +
          ",\"y\":"+y()*pixPerElement +
          ",\"orient\":\"0\""+
          ",\"type\":\"drain\""+
          ",\"key\":"+key +
          "}";
    }
  }
  private class SpontaneousNode extends Node {
    public SpontaneousNode() {
      nodeType = "link";
    }
  }

}