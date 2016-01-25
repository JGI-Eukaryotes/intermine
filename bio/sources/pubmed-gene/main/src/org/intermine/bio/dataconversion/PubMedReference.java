package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import org.intermine.model.InterMineId;
import java.util.Map;

/**
 * @author Jakub Kulaviak
 **/
public class PubMedReference
{

    private Map<InterMineId, List<InterMineId>> references;

    private InterMineId organism;

    /**
     * Constructor.
     * @param organism id of organism of which references this object carries
     * @param references references between id of gene and ids of publications in PubMed
     */
    public PubMedReference(InterMineId organism, Map<InterMineId, List<InterMineId>> references) {
        this.organism = organism;
        this.references = references;
    }

    /**
     * @return references
     * {@link #PubMedReference(InterMineId, Map)}
     */
    public Map<InterMineId, List<InterMineId>> getReferences() {
        return references;
    }

    /**
     * @param references references
     * {@link #PubMedReference(InterMineId, Map)}
     */
    public void setReferences(Map<InterMineId, List<InterMineId>> references) {
        this.references = references;
    }

    /**
     * @return organism
     * {@link #PubMedReference(InterMineId, Map)}
     */
    public InterMineId getOrganism() {
        return organism;
    }

    /**
     *
     * @param organism organism
     * {@link #PubMedReference(InterMineId, Map)}
     */
    public void setOrganism(InterMineId organism) {
        this.organism = organism;
    }
}
