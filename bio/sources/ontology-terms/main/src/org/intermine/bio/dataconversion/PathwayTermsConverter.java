package org.intermine.bio.dataconversion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

public class PathwayTermsConverter extends OntologyTermsFileConverter {
  
  private static final Logger LOG = Logger.getLogger(PathwayTermsConverter.class);
  protected HashMap<String,String> idMap = new HashMap<String,String>();
  /**
   * @param writer
   * @param model
   */
  public PathwayTermsConverter(ItemWriter writer, Model model) {
    super(writer, model);
    // we're going to process each line
    identifierKey = null;
    nameKey = null;
    descKey = null;
    endOfRecord = null;
  }
  @Override
  boolean parseLine(String line) {
    String[] fields = line.split("\\t");
    if (fields.length >= 3) {
      identifierLine = fields[0].trim();
      recordNamespace = fields[1].trim();
      nameLine = fields[2].trim();
      idMap.put(identifierLine, createOntologyTerm().getIdentifier());
    }
    return true;
  }
  @Override
  public void finalProcessing() {
    // now store the items
    for(String item : termMap.keySet()) {
      try {
        store(termMap.get(item));
      } catch (Exception e) {}
    }
  }
}
