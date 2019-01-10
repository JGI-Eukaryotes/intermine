package org.intermine.util;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * This is a map that maps from int to int. All non-existent mappings automatically map onto -1 -
 * inserting a mapping merely redefines the mapping. This class is designed to use as little RAM as
 * possible, and assumes that the mappings are from reasonably closely-spaced ints. In the case
 * where the mappings are from consecutive ints, this class will use not much more than four bytes
 * per mapping.
 *
 * @author Matthew Wakeling
 */
public class IntToIntMap
{
    private static final int INNER_MASK = 0x1fff;
    private static final int OUTER_MASK = ~INNER_MASK;
    private static final int PAGE_SIZE = INNER_MASK + 1;

    private Map<Integer, int[]> pages = new HashMap<Integer, int[]>();
    private int size = 0;

    /**
     * Constructor for this class. Creates an empty map.
     */
    public IntToIntMap() {
    }

    /**
     * Creates a mapping in the object.
     *
     * @param from any int
     * @param to any int - or -1 to effectively remove the mapping
     */
    public synchronized void put(int from, int to) {
        InterMineId pageNo = new InterMineId(from & OUTER_MASK);
        int[] page = pages.get(pageNo);
        if (page == null) {
            page = new int[PAGE_SIZE + 1];
            for (int i = 0; i < PAGE_SIZE; i++) {
                page[i] = -1;
            }
            page[PAGE_SIZE] = 0;
            pages.put(pageNo, page);
        }
        int old = page[from & INNER_MASK];
        page[from & INNER_MASK] = to;
        if (old != -1) {
            size--;
            page[PAGE_SIZE]--;
        }
        if (to != -1) {
            size++;
            page[PAGE_SIZE]++;
        }
        if (page[PAGE_SIZE] == 0) {
            pages.remove(pageNo);
        }
    }

    /**
     * Retrieves a mapping from the object
     *
     * @param from any int
     * @return an int - -1 if there is no mapping present that matches
     */
    public synchronized int get(int from) {
        InterMineId pageNo = new InterMineId(from & OUTER_MASK);
        int[] page = pages.get(pageNo);
        if (page == null) {
            return -1;
        }
        return page[from & INNER_MASK];
    }

    /**
     * Puts a mapping in the object.
     *
     * @param from an InterMineId
     * @param to any InterMineId other than -1, or null to remove a mapping
     */
    public void put(Integer from, InterMineId to) {
        if (from == null) {
            throw new NullPointerException("from is null");
        }
        int iFrom = from.intValue();
        int iTo = -1;
        if (to != null) {
            iTo = to.intValue();
            if (iTo == -1) {
                throw new IllegalArgumentException("IntToIntMap cannot handle to = -1");
            }
        }
        put(iFrom, iTo);
    }

    /**
     * Retrieves a mapping from the object.
     *
     * @param from any InterMineId
     * @return an InterMineId other than -1, or null if there is no mapping that matches
     */
    public InterMineId get(Integer from) {
        if (from == null) {
            throw new NullPointerException("from is null");
        }
        int iFrom = from.intValue();
        int to = get(iFrom);
        if (to == -1) {
            return null;
        }
        return new InterMineId(to);
    }

    /**
     * Returns the number of mappings to non-minus-one ints present.
     *
     * @return the size
     */
    public synchronized int size() {
        return size;
    }

    /**
     * Removes all mappings from the object.
     */
    public synchronized void clear() {
        pages.clear();
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString() {
        StringBuffer retval = new StringBuffer("{");
        boolean needComma = false;
        TreeSet<InterMineId> sortedKeys = new TreeSet<InterMineId>(pages.keySet());
        Iterator<InterMineId> keyIter = sortedKeys.iterator();
        while (keyIter.hasNext()) {
            InterMineId pageNo = keyIter.next();
            int pageNoInt = pageNo.intValue();
            int[] page = pages.get(pageNo);
            for (int i = 0; i < PAGE_SIZE; i++) {
                if (page[i] != -1) {
                    if (needComma) {
                        retval.append(", ");
                    }
                    needComma = true;
                    retval.append((pageNoInt + i) + " -> " + page[i]);
                }
            }
        }
        retval.append("}");
        return retval.toString();
    }
}
