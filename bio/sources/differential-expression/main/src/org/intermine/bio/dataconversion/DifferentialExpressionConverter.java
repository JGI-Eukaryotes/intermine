package org.intermine.bio.dataconversion;

import java.io.File;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class DifferentialExpressionConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "RNA-seq Differential Expression";
    private static final String DATA_SOURCE_NAME = "RNA-seq";


    private static final Logger LOG =
        Logger.getLogger(DifferentialExpressionConverter.class);

    // experiment records we will refer to by name
    private HashMap<String, Item> experimentMap = new HashMap<String,Item>();
    private HashMap<Integer,ArrayList<Item>> comparison = new HashMap<Integer,ArrayList<Item>>();
    // for now, this can only process files of 1 organism
    private Integer proteomeId = null;
    private Item organism;
    private String srcDataFile = null;
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public DifferentialExpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
      File theFile = getCurrentFile();
      if( (srcDataFile != null) && (!theFile.getName().equals(srcDataFile)) ) {
        LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
        return;
      }
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

      Iterator<String[]> tsvIter;                             
      try {                                            
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {                                           
        throw new BuildException("cannot parse file: " + getCurrentFile(), e);
      }

      try{
        // find the relevant columns and experiment names. 
        if (tsvIter.hasNext()) processHeader((String[])tsvIter.next());
        // the bulk
        processBody(tsvIter);
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing item: "+e.getMessage());
      }
    }
    
    public void processHeader(String[] fields) throws ObjectStoreException {
      // headers with spaces in them are experiments comparison
      Pattern p = Pattern.compile("^(\\S+) (\\S+)$");
      Pattern pval = Pattern.compile(".*pvalue.*");
      
      for(Integer col = 0; col < fields.length; col++) {
        Matcher m = p.matcher(fields[col]);
        if (m.find() ) {
          String exp1 = m.group(1);
          String exp2 = m.group(2);
          if (!experimentMap.containsKey(exp1) ) {
            Item e1 = createItem("RNASeqExperiment");
            e1.setAttribute("name",exp1);
            e1.setReference("organism",organism);
            store(e1);
            experimentMap.put(exp1,e1);
          }
          if (!experimentMap.containsKey(exp2) ) {
            Item e2 = createItem("RNASeqExperiment");
            e2.setAttribute("name",exp2);
            e2.setReference("organism",organism);
            store(e2);
            experimentMap.put(exp2,e2);
          }
          comparison.put(col,new ArrayList<Item>());
          comparison.get(col).add(experimentMap.get(exp1));
          comparison.get(col).add(experimentMap.get(exp2));
          if (!pval.matcher(fields[col+1]).find() ) {
            throw new BuildException("Adjacent column does not look like a pvalue.");
          }
        }
      }
    }
    
    public void processBody(Iterator<String[]> tsvIterator) throws ObjectStoreException {
      
      while( tsvIterator.hasNext()) {
        String[] fields = tsvIterator.next();
        String gene = fields[0];
        // we're going to assume this is unique in the file.
        Item g = createItem("Gene");
        g.setAttribute("primaryIdentifier",gene);
        g.setReference("organism",organism);
        store(g);
        for( Integer col : comparison.keySet()) {
          if (col <= fields.length && fields[col].length() > 0) {
            try {
              Double.valueOf(fields[col]);
              Double.valueOf(fields[col+1]);
              Item enrich = createItem("RNASeqEnrichment");
              enrich.setReference("organism",organism);
              enrich.setReference("bioentity",g);
              enrich.setReference("experiment",comparison.get(col).get(0));
              enrich.setReference("referenceExperiment",comparison.get(col).get(1));
              enrich.setAttribute("logChange",fields[col]);
              enrich.setAttribute("pvalue",fields[col+1]);
              store(enrich);
            } catch (NumberFormatException e) {
              LOG.warn("Cannot cast either "+fields[col]+" or "+fields[col+1]+" into a number.");
            }

          }
        }
      }  
    }
    public void setSrcDataFile(String file) {
      srcDataFile = file;
    }
    public void setProteomeId(String organism) {
      try {
        proteomeId = Integer.valueOf(organism);
      } catch (NumberFormatException e) {
        throw new BuildException("can't find integer proteome id for: " + organism);
      }
    }
}
