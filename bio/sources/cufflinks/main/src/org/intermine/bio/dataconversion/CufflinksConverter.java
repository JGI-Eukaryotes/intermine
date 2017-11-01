package org.intermine.bio.dataconversion;

import java.io.File;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;


/**
 * 
 * @author
 */
public class CufflinksConverter extends BioFileConverter
{

  //                                                                     
  private static final String DATASET_TITLE = "RNA-seq Expression Data";              
  private static final String DATA_SOURCE_NAME = "RNA-seq";                           

  private static final Logger LOG =
      Logger.getLogger(CufflinksConverter.class);

  // experiment records we will refer to by name
  private HashMap<String, Item> experimentMap = new HashMap<String,Item>();
  // a map from item id to experiment name.
  private HashMap<String,String> experimentIdMap = new HashMap<String,String>();
  // the 'column group' correspondence with experiment
  private HashMap<Integer,String> experimentColGroupMap = new HashMap<Integer,String>();
  // for now, this can only process files of 1 organism
  private Integer proteomeId = null;
  private Item organism;
  // bioentities we record data about
  private HashMap<String,HashMap<String,Item> > bioentityMap = new HashMap<String, HashMap<String, Item> >();
  // the score data we record data about
  // the keys are bioentity type (gene or isoform), then bioentity identifier, then the experiment
  private HashMap<String,HashMap<String,HashMap<String,Item>>> scoreMap = 
      new HashMap<String, HashMap<String,HashMap<String,Item>>>();
  // keepers of the outlier data.
  private HashMap<String,HashMap<String,String>> locusRating = new HashMap<String,HashMap<String,String>>();
  private HashMap<String,HashMap<String,String>> libraryRating = new HashMap<String,HashMap<String,String>>();


  private final String[] expectedFPKMHeaders = { "tracking_id" , "class_code",
      "nearest_ref_id" , "gene_id" , "gene_short_name",
      "tss_id" , "locus" , "length" , "coverage"};
  private final String[] expectedFPKMSuffices = {"_FPKM", "_conf_lo", "_conf_hi", "_status" };
  private final String[] expectedCountHeaders = { "tracking_id"};
  private final String[] expectedCountSuffices = {"_count","_count_variance","_count_uncertainty_var","_count_dispersion_var","_status"};


  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public CufflinksConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    bioentityMap.put("Gene", new HashMap<String,Item>());
    bioentityMap.put("MRNA", new HashMap<String,Item>());
    scoreMap.put("Gene", new HashMap<String,HashMap<String,Item>>());
    scoreMap.put("MRNA", new HashMap<String,HashMap<String,Item>>());
  }

  /**
   * The main even. Read and process a cufflinks file.
   * We read each of the files, then store the results in the close() method.
   * {@inheritDoc}
   */ 

  public void process(Reader reader) throws Exception {
    File theFile = getCurrentFile();                 
    LOG.info("Processing file "+theFile.getName()+"...");

    if (organism==null) {
      // we need to register the organism
      if (proteomeId != null ) {
        organism = createItem("Organism");
        organism.setAttribute("proteomeId", proteomeId.toString());
        try {
          store(organism);
        } catch (ObjectStoreException e) {
          throw new RuntimeException("failed to store organism with proteomeId: "
              + proteomeId, e);
        }
      } else {
        throw new BuildException("No proteomeId specified.");
      }
    }

    if (theFile.getName().endsWith("genes.fpkm_tracking")) {
      processCufflinksFile(reader,"FPKM","Gene");
    } else if (theFile.getName().endsWith("isoforms.fpkm_tracking")) {
      processCufflinksFile(reader,"FPKM","MRNA");
    } else if (theFile.getName().endsWith("genes.count_tracking")) {
      processCufflinksFile(reader,"Count","Gene");
    } else if (theFile.getName().endsWith("isoforms.count_tracking")) {
      processCufflinksFile(reader,"Count","MRNA");
    } else if (theFile.getName().endsWith("outliers.tsv")) {
      processOutliersFile(reader);
    } else if (theFile.getName().endsWith("experiments.info")) {
      processSampleFile(reader);
    } else {
      LOG.info("Ignoring file "+theFile.getName()+".");
    }
  }

  public void close() throws Exception
  {
    // store the experiments
    for(Item exp: experimentMap.values()) {
      try {
        store(exp);
      } catch (ObjectStoreException e) {
        throw new BuildException("Cannot save experiment " + exp.getAttribute("name"));
      }
    }
    // Now store scores. First an iterator over the types
    Iterator<Map.Entry<String,HashMap<String, HashMap<String,Item> > > > typeIterator = scoreMap.entrySet().iterator();
    while (typeIterator.hasNext() ) {
      // look at each (name,item) for that type
      Entry<String, HashMap<String, HashMap<String, Item>>> theType = typeIterator.next();
      Iterator<Map.Entry<String,HashMap<String, Item>>> idMapIterator = theType.getValue().entrySet().iterator();
      while (idMapIterator.hasNext() ) {
        Entry<String, HashMap<String, Item>> theBioentity = idMapIterator.next();
        Iterator<Map.Entry<String,Item>> scoreMapIterator = theBioentity.getValue().entrySet().iterator();
        while(scoreMapIterator.hasNext() ) {
          Entry<String, Item> theScore = scoreMapIterator.next();
          String geneName = theBioentity.getKey();
          Reference expRef = theScore.getValue().getReference("experiment");
          String expName = experimentIdMap.get(expRef.getRefId());
          if (theType.getKey().equals("Gene")) {
            if (locusRating.containsKey(geneName) && locusRating.get(geneName).containsKey(expName) ){
              theScore.getValue().setAttribute("locusExpressionLevel",locusRating.get(geneName).get(expName));         
            }
            if (libraryRating.containsKey(geneName) && libraryRating.get(geneName).containsKey(expName) ){
              theScore.getValue().setAttribute("libraryExpressionLevel",libraryRating.get(geneName).get(expName));
            }
            if(Double.parseDouble(theScore.getValue().getAttribute("abundance").getValue()) == 0.) {
              theScore.getValue().setAttribute("locusExpressionLevel","Not Expressed");
              theScore.getValue().setAttribute("libraryExpressionLevel","Not Expressed");
            }
          }
          try {
            store(theScore.getValue());
          } catch (Exception e) {
            throw new BuildException("Problem when storing item: " +e);
          }
        }
      }
    } 
  }

  private void processSampleFile(Reader reader) throws BuildException {
    Iterator<?> tsvIter;
    try {
      tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
    } catch (Exception e) {
      throw new BuildException("Cannot parse file: " + getCurrentFile(),e);
    }
    int ctr = 0;
    while (tsvIter.hasNext() ) {
      ctr++;
      String[] fields = (String[]) tsvIter.next();
      if (fields.length > 1) {
        Item sample = createExperiment(cleanUpExperimentName(fields[0]));
        setIfNotNull(sample,"experimentGroup",fields[1]);
        if (fields.length > 2) setIfNotNull(sample,"description",fields[2]);
        if (fields.length > 3) setIfNotNull(sample,"url",fields[3]);
      }
    }
  }


  private void processOutliersFile(Reader reader) throws BuildException {  

    Iterator<?> tsvIter;                             
    try {                                            
      tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
    } catch (Exception e) {                                           
      throw new BuildException("cannot parse file: " + getCurrentFile(), e);
    }

    int lineNumber = 0;                                        

    while (tsvIter.hasNext()) {
      lineNumber++;
      String[] fields = (String[]) tsvIter.next();
      if (fields.length != 3) {
        throw new BuildException("Unexpected number of columns in outlier file at line "+lineNumber);
      }
      if (fields[0].equals("LocusLow")) {
        // first gene, then experiment
        if (!locusRating.containsKey(fields[1])) locusRating.put(fields[1],new HashMap<String,String>());
        locusRating.get(fields[1]).put(cleanUpExperimentName(fields[2]),"Low");   
      } else if (fields[0].equals("LocusHigh")) { 
        // first gene, then experiment
        if (!locusRating.containsKey(fields[1])) locusRating.put(fields[1],new HashMap<String,String>());
        locusRating.get(fields[1]).put(cleanUpExperimentName(fields[2]),"High");       
      } else if (fields[0].equals("ExperimentLow")) {        
        // first experiment, then gene
        if (!libraryRating.containsKey(fields[2])) libraryRating.put(fields[2],new HashMap<String,String>());
        libraryRating.get(fields[2]).put(cleanUpExperimentName(fields[1]),"Low");
      } else if (fields[0].equals("ExperimentHigh")) {       
        // first experiment, then gene
        if (!libraryRating.containsKey(fields[2])) libraryRating.put(fields[2],new HashMap<String,String>());
        libraryRating.get(fields[2]).put(cleanUpExperimentName(fields[1]),"High");
      }
    }
  }

  private void processCufflinksFile(Reader reader, String fileType, String bioentityType)
      throws BuildException, ObjectStoreException   {  

    int colGroupSize = fileType.equals("FPKM")?expectedFPKMSuffices.length:
      expectedCountSuffices.length;
    String[] expectedHeaders = fileType.equals("FPKM")?
        (String[])expectedFPKMHeaders.clone():(String[])expectedCountHeaders.clone();
        Iterator<?> tsvIter;                             
        try {                                            
          tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {                                           
          throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        int lineNumber = 0;                                        

        while (tsvIter.hasNext()) {
          String[] fields = (String[]) tsvIter.next();

          if (lineNumber == 0) {
            processHeaders(fields,fileType);
          } else {
            String primaryId = fields[0];
            if (StringUtils.isEmpty(primaryId)) {
              break;
            }
            // register the gene/isoform is not see already.
            if(!bioentityMap.get(bioentityType).containsKey(primaryId) ) {
              Item i = createItem(bioentityType);
              i.setAttribute("primaryIdentifier", primaryId);
              i.setReference("organism", organism);
              store(i);
              bioentityMap.get(bioentityType).put(primaryId, i);
            }

            // scan and process all scores
            for (int i = expectedHeaders.length; i < fields.length;) {
              // the 'group' of columns in 1 experiment
              Integer colGroup = new Integer( (i-expectedHeaders.length)/colGroupSize);
              if (!experimentColGroupMap.containsKey(colGroup)){
                throw new BuildException("Unexpected number of columns in " + getCurrentFile() +
                    " at line " + lineNumber);
              }
              String experiment = experimentColGroupMap.get(colGroup);
              if (!scoreMap.get(bioentityType).containsKey(primaryId) ) {
                scoreMap.get(bioentityType).put(primaryId,new HashMap<String,Item>());
              }
              if(!scoreMap.get(bioentityType).get(primaryId).containsKey(experiment)){
                Item score = createItem("RNASeqExpression");
                scoreMap.get(bioentityType).get(primaryId).put(experiment,score);
              }
              Item score = scoreMap.get(bioentityType).get(primaryId).get(experiment);
              if( fileType.equals("FPKM") ) {
                score.setAttribute("method","cufflinks");
                try {
                  if (!fields[i  ].trim().isEmpty() ) score.setAttribute("abundance",fields[i].trim());
                  if (!fields[i+1].trim().isEmpty() ) score.setAttribute("conflo",fields[i+1].trim());
                  if (!fields[i+2].trim().isEmpty() ) score.setAttribute("confhi",fields[i+2].trim());
                  if (!fields[i+3].trim().isEmpty() ) score.setAttribute("status",fields[i+3].trim());
                } catch (ArrayIndexOutOfBoundsException e) {
                  throw new BuildException("Incorrect number of fields (" + i + " to " + (i+2) + ") at line " + lineNumber
                      + " in " + getCurrentFile() );
                }
                score.setReference("bioentity", bioentityMap.get(bioentityType).get(primaryId));
                score.setReference("experiment", experimentMap.get(experimentColGroupMap.get(colGroup)));
              } else {
                score.setAttribute("method","cufflinks");
                try {
                  if (!fields[i  ].trim().isEmpty() ) score.setAttribute("count",fields[i].trim());
                  if (!fields[i+1].trim().isEmpty() ) score.setAttribute("countvariance",fields[i+1].trim());
                  if (!fields[i+2].trim().isEmpty() ) score.setAttribute("countuncertaintyvar",fields[i+2].trim());
                  if (!fields[i+3].trim().isEmpty() ) score.setAttribute("countdispersionvar",fields[i+3].trim());
                } catch (ArrayIndexOutOfBoundsException e) {
                  throw new BuildException("Incorrect number of fields (" + i + " to " + (i+3) + ") at line " + lineNumber
                      + " in " + getCurrentFile() );
                }
                score.setReference("bioentity", bioentityMap.get(bioentityType).get(primaryId));
                score.setReference("experiment", experimentMap.get(experimentColGroupMap.get(colGroup)));
              }
              i+=colGroupSize;
            }
          }
          lineNumber++;
        }
  }


  private void processHeaders(String[] headers,String type) throws BuildException {

    int colGroupSize;
    String[] expectedHeaders;
    String[] expectedSuffices;

    if (type.equals("FPKM") ) {
      colGroupSize = expectedFPKMSuffices.length;
      expectedHeaders = (String[])expectedFPKMHeaders.clone();
      expectedSuffices = (String[])expectedFPKMSuffices.clone();
    } else if (type.equals("Count") ) {
      colGroupSize = expectedCountSuffices.length;
      expectedHeaders = (String[])expectedCountHeaders.clone();
      expectedSuffices = (String[])expectedCountSuffices.clone();
    } else {
      throw new BuildException("Unexpected type of header: " + type);
    }

    String experimentName = null;
    for(int i=0;i<headers.length;i++) {
      if ( i<expectedHeaders.length ) {
        if( !headers[i].equals(expectedHeaders[i]) ) {
          throw new BuildException("Unexpected header "+ headers[i] + " expected " +
              expectedHeaders[i] + " in " + getCurrentFile());
        }
      } else {
        int which = (i - expectedHeaders.length)%colGroupSize;
        int colGroup = (i - expectedHeaders.length)/colGroupSize;
        if ( which == 0) {
          // should be experimentName + suffix. We may need to register experiment
          if (!headers[i].endsWith(expectedSuffices[0]) ) {
            throw new BuildException("Unexpected header " + headers[i] +
                " expected experimentName"+ expectedSuffices[0] +" in "+getCurrentFile());
          }
          experimentName = headers[i].substring(0,headers[i].lastIndexOf(expectedSuffices[0]));
          String cleanedExperimentName = cleanUpExperimentName(experimentName);
          experimentColGroupMap.put(colGroup,cleanedExperimentName);
          createExperiment(cleanedExperimentName);
        }
        if (!headers[i].equals(experimentName + expectedSuffices[which])){
          throw new BuildException("Unexpected header " + headers[i] +
              " expected experimentName" + expectedSuffices[which] + " in "+getCurrentFile());
        }
      }
    }
  }
  /*
   * convert from a cufflinks style name (with underscores) to a more readable form.
   * convert single underscores to spaces
   * convert N (N>1) underscore to N-1 underscores and no space.
   */
  private String cleanUpExperimentName(String s) {
    StringBuffer sb = new StringBuffer();
    for( String bit: s.split("_") ) {
      // a bit of zero-length means there were 2 underscores. Replace
      // this with 1
      if(bit.length()==0) {
        sb.append("_");
      } else {
        if (sb.length() > 0  && !sb.toString().endsWith("_") ) sb.append(" ");
        sb.append(bit);
      }
    }
    return sb.toString();
  }

  /*
   * If we have not seen this experiment before, create (but do not store)
   * one experiment for the current organism. 
   */

  private Item createExperiment(String name) {
    if (!experimentMap.containsKey(name) ) {
      Item experiment = createItem("RNASeqExperiment");
      experiment.setAttribute("name",name);
      experiment.setReference("organism",organism);
      experimentMap.put(name,experiment);
      experimentIdMap.put(experiment.getIdentifier(),name);
    }
    return experimentMap.get(name);
  }
  void setIfNotNull(Item s,String field,String value) {
    if (value != null && value.trim().length() > 0) {
      s.setAttribute(field,value);
    }
  }
  public void setProteomeId(String organism) {
    try {
      proteomeId = Integer.valueOf(organism);
    } catch (NumberFormatException e) {
      throw new RuntimeException("can't find integer proteome id for: " + organism);
    }
  }
}

