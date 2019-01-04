package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 Phytozome
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;


/**
 * A file converter that reads terms from an EMBL (or EMBL-ish) formatted file,
 * or a simple text format, makes entries in the OntologyTerm table, adding
 * entries for the identifier (accession number), name, namespace and
 * description. For EMBL formatted files, the scanner looks for key-value pairs
 * to add. The accession, name, namespace and description fields must start at
 * with a specific key at the start of line and be separated by whitespace, Each
 * individual record is delimited with some sort of end of record ("//"). For
 * simpler text files, all information is presumed to be in 1 line of the text
 * file. Calling a subclassed parseLine method extracts the information and
 * stores the record.
 * 
 * @author JWCarlson
 */
public class OntologyTermsFileConverter extends BioFileConverter
{
    //
    private static final Logger LOG = Logger.getLogger(OntologyTermsFileConverter.class);
    protected Item ontology = null;
    protected String dataSetTitle = null;
    protected String dataSetRefId = null;
    protected String dataSourceName = null;
    protected String dataSourceRefId = null;
    protected String dataSetVersion = null;
    protected String dataSetURL = null;
    protected String srcDataFile = null;
    protected String srcDataDir = null;

    protected String identifierLine = null;
    protected String descriptionLine = null;
    protected String nameLine = null;
    // the namespace may be set by a property.
    protected String globalNamespace = null;
    // Or, it could be set on a record level. Possibly by a subclass. We're just going to
    // expose the Stirng to allow this to be set here or in a cleanup routine.
    String recordNamespace = null;
    // for files that get parsed with (mainly) one value per line,
    // these are the keys and the end-of-record delimiter
    // the normal behavior is to scan and look for the keys, then storing
    // the term when the endOfRecord key is found. But is perfectly acceptable
    // to read a line, parse everything from that line and then store the record in
    // the line parser routine
    protected String identifierKey = "^ACC.*";
    protected String nameKey = "^NAME.*";
    protected String descKey = "^DESC.*";
    protected String namespaceKey = null;
    protected String identifierReplacement = "^ACC\\s*";
    protected String nameReplacement = "^NAME\\s*";
    protected String descReplacement = "^DESC\\s*";
    protected String namespaceReplacement = null;
    protected String endOfRecord = "^//.*";
    // inserted items. The key is the identifier that is read. It is
    // unique within this ontology.
    protected HashMap<String,Item> termMap = new HashMap<String,Item>();
    protected HashSet<ParentChild> parentChildren = new HashSet<ParentChild>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OntologyTermsFileConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }   
    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
      File theFile = getCurrentFile();
      // if we're only going to parse 1 file. It should match srcDataFile
      if( (srcDataFile != null) && (!theFile.getName().equals(srcDataFile)) ) {
        LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
      } else {
        // register and store the ontology.
        if (ontology == null) {
          ontology = createItem("Ontology");
          ontology.addAttribute(new Attribute("name",dataSetTitle));
          setIfNotNull(ontology,"url",dataSetURL);
          store(ontology);
        }

        BufferedReader in = new BufferedReader(new FileReader(theFile));
        String line;

        int ctr = 0;
        while ( (line = in.readLine()) != null) {
          ctr++;
          // if subclasses, parseLine will handle all the work and return true.
          // otherwise, look for keys
          if (!parseLine(line) ) {
            // end of record indicator
            if(endOfRecord != null && line.matches(endOfRecord) ) {
              if (identifierLine != null ) { //&& nameLine != null) {
                // protect against stray end-of-record indicators
                // and make sure identifierLine (at least) is set.
                storeRecord();
              }
              identifierLine = null;
              nameLine = null;
              descriptionLine = null;
              recordNamespace = null;
            } else {
              if (identifierKey != null && line.matches(identifierKey)) {
                parseIdentifier(line);
              } else if (descKey != null && line.matches(descKey)) {
                parseDescription(line);
              } else if (nameKey != null && line.matches(nameKey)) {
                parseName(line);
              } else if (namespaceKey != null && line.matches(namespaceKey)) {
                parseNamespace(line);
              }
            }
          }
          if ((ctr%100000) == 0) {
            LOG.info("Processed " + ctr + " lines...");
          }
        }
        LOG.info("Processed " + ctr + " lines.");
        // now store the parent-child relationships
        for( Object pair : parentChildren.toArray() ) {
          Item rel = createItem("OntologyRelation");
          rel.setAttribute("relationship", "part_of");
          rel.setAttribute("redundant","false");
          rel.setAttribute("direct",((ParentChild)pair).getDirect()?"true":"false");
          rel.setReference("childTerm", termMap.get(((ParentChild)pair).getChild()));
          rel.setReference("parentTerm", termMap.get(((ParentChild)pair).getParent()));
          store(rel);
        }
        LOG.info("Added "+parentChildren.size()+" relationships.");
        finalProcessing();
        in.close();
      }
    }
    Item createOntologyTerm() {
      // create the item. Assuming everything is parsed. And then storing it later
      Item ontTerm = createItem("OntologyTerm");
      ontTerm.setAttribute("identifier",cleanId(identifierLine));
      setIfNotNull(ontTerm,"name", cleanName(nameLine));
      setIfNotNull(ontTerm,"description",cleanDescription(descriptionLine));
      // first try the global namespace.
      setIfNotNull(ontTerm,"namespace",globalNamespace);
      // something set in the record overrides something in the property/id parser
      setIfNotNull(ontTerm,"namespace",recordNamespace);
      ontTerm.setReference("ontology", ontology.getIdentifier());
      termMap.put(ontTerm.getAttribute("identifier").getValue(),ontTerm);
      LOG.info("Adding "+ontTerm.getAttribute("identifier").getValue()+" to termMap");
      // be sure these are nulled after insertion.
      identifierLine = null;
      nameLine = null;
      descriptionLine = null;
      recordNamespace = null;
      return ontTerm;
    }
    /*
     *  store the term using the current ontology
     */
    public String storeRecord() throws ObjectStoreException {
      if (termMap.containsKey(cleanId(identifierLine))) {
        // we've seen this before.
        return null;
      }
      Item ontTerm = createItem("OntologyTerm");
      ontTerm.setAttribute("identifier",cleanId(identifierLine));
      setIfNotNull(ontTerm,"name", cleanName(nameLine));
      setIfNotNull(ontTerm,"description",cleanDescription(descriptionLine));
      // first try the global namespace.
      setIfNotNull(ontTerm,"namespace",globalNamespace);
      // something set in the record overrides something in the property/id parser
      setIfNotNull(ontTerm,"namespace",recordNamespace);
      ontTerm.setReference("ontology", ontology.getIdentifier());
      store(ontTerm);
      termMap.put(ontTerm.getAttribute("identifier").getValue(),ontTerm);
      // be sure these are nulled after insertion.
      identifierLine = null;
      nameLine = null;
      descriptionLine = null;
      recordNamespace = null;
      return ontTerm.getIdentifier();
    }
    /*
     * parse the line/id/name/description/namespace. These may
     * be overridden in subclasses.
     */
    boolean parseLine(String line) {
      return false;
    }
    void parseIdentifier(String line) {
      identifierLine = line.replaceAll(identifierReplacement,"").trim();
    }
    void parseDescription(String line) {
      // we're going to allow for continuation lines.
      if (descriptionLine != null) {
        descriptionLine = descriptionLine.concat(" ");
        descriptionLine = descriptionLine.concat(line.replaceAll(descReplacement,"").trim());
      } else {
        descriptionLine = line.replaceAll(descReplacement,"").trim();
      }          
    }
    void parseName(String line) {
      // we're not going to have continuation lines here.
      // still don't know how to handle multiple names.
      nameLine = line.replaceAll(nameReplacement,"").trim(); 
    }
    void parseNamespace(String line) {
      recordNamespace = line.replaceAll(namespaceReplacement,"").trim();
    }
    void finalProcessing() {
      // whatever needs to be done before closing the file
    }
    /* do final cleanup on id, name and string.
     * Depending on the particulars of the file contents, these
     * fields may need to be cleaned up in a subclass. Sometimes
     * the cleanup can only be done after we have all values.
     */
    String cleanId(String id) {
      if (id==null) return null;
      try {
        return new String(id.getBytes("UTF-8"),"UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new BuildException("Cannot get UTF-8 encoded form of id "+id);
      }
    }
    String cleanName(String name) {
      if (name==null) return null;
      try {
        return new String(name.getBytes("UTF-8"),"UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new BuildException("Cannot get UTF-8 encoded form of name "+name);
      }
    }
    String cleanDescription(String desc) {
      if (desc==null) return null;
      try {
        return new String(desc.getBytes("UTF-8"),"UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new BuildException("Cannot get UTF-8 encoded form of description "+desc);
      }
    }
    /**
     * Getters and setters for ontology attribures
     * 
     */
    public void setDataSetTitle(String title) {
      dataSetTitle = title;
      if (dataSourceRefId != null) {
        dataSetRefId = getDataSet(dataSetTitle,dataSourceRefId);
      }
    }
    public void setDataSetVersion(String version) {
      dataSetVersion = version;
    }
    public void setDataSetURL(String URL) {
      dataSetURL = URL;
    }
    public void setDataSourceName(String name) {
      dataSourceName = name;
      dataSourceRefId = getDataSource(dataSourceName);
      if (dataSourceRefId!=null) {
        dataSetRefId = getDataSet(dataSetTitle,dataSourceRefId);
      }
    }
    public String getDataSetTitle() {
      return dataSetTitle;
    }
    public String getDataSetVersion() {
      return dataSetVersion;
    }
    public String getDataSetURL() {
      return dataSetURL;
    }
    public String getDataSourceName() {
      return dataSourceName;
    }
    public void setSrcDataFile(String file) {
      srcDataFile = file;
    }
    public void setSrcDataDir(String dir) {
      srcDataDir = dir;
    }
    
    /* 
     * set an attribute if it's not null and not an empty string
     */
    void setIfNotNull(Item s,String field,String value) {
      if (value != null && value.trim().length() > 0) {
        s.setAttribute(field,value);
      }
    }

    protected class ParentChild {
      private String parentIdentifier;
      private String childIdentifier;
      private Boolean direct;
      public ParentChild(String parent, String child, boolean isDirect) {
        parentIdentifier = parent;
        childIdentifier = child;
        direct = isDirect;
      }
      public String getParent() { return parentIdentifier; }
      public String getChild() { return childIdentifier; }
      public Boolean getDirect() { return direct; }
    }
}
