/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.query;

import junit.framework.TestCase;

public class QueryClassTest extends TestCase
{
    private final Class CLASS = String.class;
    private QueryClass qc;

    public QueryClassTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(CLASS);
    }

    public void testGetType() {
        assertEquals(CLASS, qc.getType());
    }
}
