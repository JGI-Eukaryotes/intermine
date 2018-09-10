package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.lang.NumberFormatException;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.SymbolList;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.metadata.StringUtil;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;

/**
 * Exports DNA sequences of given genomic regions in FASTA format.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSequenceExporter
{
    private ObjectStore os;
    private OutputStream out;
    
    private static final Logger LOG = Logger.getLogger(GenomicRegionSequenceExporter.class);

    /**
     * Instructor
     *
     * @param os ObjectStore
     * @param out output stream
     */
    public GenomicRegionSequenceExporter(ObjectStore os, OutputStream out) {
        this.os = os;
        this.out = out;
    }

    /**
     * DO export
     * @param grList a list of GenomicRegion objects
     * @throws Exception ex
     */
    public void export(List<GenomicRegion> grList) throws Exception {
        GenomicRegion aRegion = grList.get(0);
        Organism org = (Organism) DynamicUtil.createObject(Collections
                .singleton(Organism.class));
        LOG.info("Exporting sequence for "+aRegion.toString());
        // phytomine tweak. An integer is a proteome id
        try {
          org.setProteomeId(Integer.parseInt(aRegion.getOrganism()));
          org = os.getObjectByExample(org, Collections.singleton("proteomeId"));
        } catch (NumberFormatException e) {
          org.setShortName(aRegion.getOrganism());
          org = os.getObjectByExample(org, Collections.singleton("shortName"));
        }


        for (GenomicRegion gr : grList) {
            Chromosome chr = (Chromosome) DynamicUtil.createObject(
                    Collections.singleton(Chromosome.class));
            chr.setPrimaryIdentifier(gr.getChr());
            chr.setOrganism(org);

            chr = os.getObjectByExample(chr,
                        new HashSet<String>(Arrays.asList("primaryIdentifier", "organism")));

            String chrResidueString = chr.getSequence().getResidues().toString();

            int chrLength = chr.getLength();
            int start;
            int end;

            if (gr.getExtendedRegionSize() > 0) {
              start = gr.getExtendedStart();
              end = gr.getExtendedEnd();
            } else {
              start = gr.getStart();
              end = gr.getEnd();
            }
            /* 
             * if start > end, we'll interpret this as "gimme sequence 
             * from end to start but rev-comp'ed"
             */
            boolean is_revcomp = false;
            if (start > end) {
              is_revcomp = true;
              int save = start;
              start = end;
              end = save;
            }

            // ensure range.
            start = Math.max(Math.min(start, chrLength),1);
            end = Math.max(Math.min(end, chrLength),1);

            List<String> headerBits = new ArrayList<String>();
            headerBits.add(gr.getChr() + ":" + start + ".." + end);
            headerBits.add(end - start + 1 + "bp");
            if (is_revcomp) headerBits.add("(reverse complement)");
            headerBits.add(org.getShortName());
            String header = StringUtil.join(headerBits, " ");

            String seqName = "genomic_region_" + gr.getChr() + "_"
                    + start + "_" + end + "_"
                    + gr.getOrganism().replace("\\. ", "_");

            Sequence chrSeg;
            
            if (is_revcomp) {
              chrSeg = DNATools.createDNASequence(
                  DNATools.reverseComplement(
                      DNATools.createDNA(chrResidueString.substring(start - 1, end))).seqString(),
                      seqName);
            } else {
              chrSeg = DNATools.createDNASequence(
                  chrResidueString.substring(start - 1, end),
                  seqName);
            }
            chrSeg.getAnnotation().setProperty(
                    FastaFormat.PROPERTY_DESCRIPTIONLINE, header);

            // write it out
            SeqIOTools.writeFasta(out, chrSeg);
        }
        out.flush();
    }
}
