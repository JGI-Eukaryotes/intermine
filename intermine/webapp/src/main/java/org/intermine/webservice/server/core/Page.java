package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineId;

/**
 * Parameter class for passing start and limit arguments to
 * iterators.
 * @author Alex Kalderimis
 *
 */
public class Page
{
    private final int start;
    private final InterMineId size;

    /**
     * Construct a new page.
     * @param start The index of the first row to return.
     * @param size The maximum number of rows to return.
     */
    public Page(int start, InterMineId size) {
        this.start = start;
        this.size = size;
    }

    /**
     * Construct a new page, going from a certain row to the end
     * of the result set.
     * @param start The index of the first result to return.
     */
    public Page(int start) {
        this(start, null);
    }

    /**
     * Get the index of the first row that is requested.
     * @return The index of the first row to return.
     */
    public int getStart() {
        return start;
    }

    /**
     * @return The requested size, or NULL if all results are
     * requested.
     */
    public InterMineId getSize() {
        return size;
    }

    /**
     * @return The index of the last result to be returned, or NULL
     * if all results are requested.
     */
    public InterMineId getEnd() {
        if (size == null) {
            return null;
        } else {
            return InterMineId.valueOf(start + size.intValue());
        }
    }

    /**
     * @param index An index to test.
     * @return Whether or not the given index lies within the page.
     */
    public boolean withinRange(int index) {
        InterMineId end = getEnd();
        if (end != null && index >= end.intValue()) {
            return false;
        }
        return index >= start;
    }
}
