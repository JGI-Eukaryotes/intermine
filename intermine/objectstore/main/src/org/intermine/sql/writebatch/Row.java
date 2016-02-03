package org.intermine.sql.writebatch;

import org.intermine.model.InterMineId;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Stores two ints.
 *
 * @author Matthew Wakeling
 */
public class Row implements Comparable<Row>
{
    private InterMineId left;
    private InterMineId right;

    /**
     * Constructor.
     *
     * @param interMineId the left integer
     * @param interMineId2 the right integer
     */
    public Row(InterMineId interMineId, InterMineId interMineId2) {
        this.left = interMineId;
        this.right = interMineId2;
    }

    /**
     * Returns left.
     *
     * @return left
     */
    public InterMineId getLeft() {
        return left;
    }

    /**
     * Returns right.
     *
     * @return right
     */
    public InterMineId getRight() {
        return right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if ((o instanceof Row) && (((Row) o).left.equals(left)) && (((Row) o).right.equals(right))) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return left.intValue() + (1013 * right.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Row o) {
        int retval = left.nativeValue() - o.left.nativeValue();
        if (retval == 0) {
            retval = right.nativeValue() - o.right.nativeValue();
        }
        return retval;
    }
}
