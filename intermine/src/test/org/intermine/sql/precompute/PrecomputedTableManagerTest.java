/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.sql.precompute;

import junit.framework.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.flymine.util.DatabaseUtil;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.sql.query.*;

public class PrecomputedTableManagerTest extends TestCase
{
    private PrecomputedTable pt1;
    private Database database;

    public PrecomputedTableManagerTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {

        database = DatabaseFactory.getDatabase("db.unittest");
        Query q1 = new Query();
        Table t = new Table("tabletest");
        Constant c = new Constant("50");
        Field f1 = new Field("col1", t);
        Field f2 = new Field("col2", t);
        SelectValue sv1 = new SelectValue(f1, null);
        SelectValue sv2 = new SelectValue(f2, null);
        q1.addFrom(t);
        q1.addSelect(sv1);
        q1.addSelect(sv2);
        q1.addWhere(new Constraint(f1, Constraint.LT, c));
        q1.addOrderBy(f1);

        pt1 = new PrecomputedTable(q1, "precomp1");

    }

    protected void createTable() throws Exception {
        // Set up some tables in the database
        Connection con = database.getConnection();
        Statement stmt = con.createStatement();
        stmt.addBatch("CREATE TABLE tabletest(col1 int, col2 int)");
        for (int i = 1; i<100; i++) {
            stmt.addBatch("INSERT INTO tabletest VALUES(" + i + ", " + (101-i) + ")" );
        }
        stmt.executeBatch();
        con.commit();
        con.close();

    }

    protected void deleteTable() throws Exception {
        Connection con = database.getConnection();
        Statement stmt = con.createStatement();
        stmt.addBatch("DROP TABLE tabletest");
        stmt.addBatch("DROP TABLE precompute_index");
        stmt.executeBatch();
        con.commit();
        con.close();
    }

    public void testNullDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testValidDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        }
        catch (RuntimeException e) {
            fail("No exception should be thrown");
        }
    }

    public void testAddNull() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            ptm.add(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testDeleteNullTable() throws Exception {
        try {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            ptm.delete((PrecomputedTable) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGetInstanceForNullDatabase() throws Exception {
        try {
            PrecomputedTableManager ptm = PrecomputedTableManager.getInstance((Database) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGetInstance() throws Exception {
        try {
            PrecomputedTableManager ptm1 = PrecomputedTableManager.getInstance(database);
            PrecomputedTableManager ptm2 = PrecomputedTableManager.getInstance(database);
            assertTrue(ptm1 == ptm2);
        } finally {
            // Make sure we don't muck up any tests in the future by having a PrecomputedTableManager lying around with no index table to match it.
            PrecomputedTableManager.instances = new HashMap();
        }
    }

    public void testAddNew() throws Exception {
        synchronized (pt1) {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm.add(pt1);
                assertTrue(ptm.getPrecomputedTables().contains(pt1));
                assertTrue(DatabaseUtil.tableExists(database.getConnection(), "precomp1"));
            }
            finally {
                ptm.delete(pt1);
                deleteTable();
            }
        }
    }


    public void testDelete() throws Exception {
        synchronized (pt1) {
            PrecomputedTableManager ptm = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm.add(pt1);
                ptm.delete(pt1);
                assertTrue(!(ptm.getPrecomputedTables().contains(pt1)));
                assertTrue(!(DatabaseUtil.tableExists(database.getConnection(), "precomp1")));
            }
            finally {
                deleteTable();
            }
        }
    }

    public void testAddInvalid() throws Exception {
        PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        try {
            ptm.add(new PrecomputedTable(new Query("select table.blah from table"), "precomp1"));
            fail("Expected: SQLException");
        }
        catch (SQLException e) {
        }
    }

    public void testDeleteInvalid() throws Exception {
        PrecomputedTableManager ptm = new PrecomputedTableManager(database);
        try {
            ptm.delete(new PrecomputedTable(new Query(), "tablenotthere"));
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }


    public void testExistingTables() throws Exception {
        synchronized (pt1) {
            PrecomputedTableManager ptm1 = new PrecomputedTableManager(database);
            try {
                createTable();
                ptm1.add(pt1);

                PrecomputedTableManager ptm2 = new PrecomputedTableManager(database);

                PrecomputedTable pt2 = (PrecomputedTable) ptm2.getPrecomputedTables().iterator().next();

                assertEquals(pt1, pt2);
            }
            finally {
                ptm1.delete(pt1);
                deleteTable();
            }
        }
    }
}
