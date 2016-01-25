package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.intermine.sql.Database;
import org.intermine.model.InterMineId;
import org.intermine.sql.DatabaseFactory;

/**
 * TestCase for doing tests on the BatchWriter system.
 *
 * @author Matthew Wakeling
 */
public abstract class BatchWriterTestCase extends TestCase
{
    public BatchWriterTestCase(String arg) {
        super(arg);
    }

    public void testOpsWithId() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (11, 101)");
            s.addBatch("INSERT INTO table1 VALUES (12, 102)");
            s.addBatch("INSERT INTO table1 VALUES (22, 202)");
            s.addBatch("INSERT INTO table1 VALUES (32, 302)");
            s.addBatch("INSERT INTO table1 VALUES (13, 103)");
            s.addBatch("INSERT INTO table1 VALUES (23, 203)");
            s.addBatch("INSERT INTO table1 VALUES (33, 303)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            batch.addRow(con, "table1", new InterMineId(14), colNames, new Object[] {new InterMineId(14), new InterMineId(104)});
            batch.addRow(con, "table1", new InterMineId(15), colNames, new Object[] {new InterMineId(15), new InterMineId(105)});
            batch.addRow(con, "table1", new InterMineId(25), colNames, new Object[] {new InterMineId(25), new InterMineId(205)});
            batch.addRow(con, "table1", new InterMineId(35), colNames, new Object[] {new InterMineId(35), new InterMineId(305)});
            batch.deleteRow(con, "table1", "col1", new InterMineId(11));
            batch.deleteRow(con, "table1", "col1", new InterMineId(12));
            batch.deleteRow(con, "table1", "col1", new InterMineId(22));
            batch.deleteRow(con, "table1", "col1", new InterMineId(32));
            batch.deleteRow(con, "table1", "col1", new InterMineId(14));
            batch.deleteRow(con, "table1", "col1", new InterMineId(24));
            batch.deleteRow(con, "table1", "col1", new InterMineId(34));
            batch.addRow(con, "table1", new InterMineId(12), colNames, new Object[] {new InterMineId(12), new InterMineId(112)});
            batch.addRow(con, "table1", new InterMineId(22), colNames, new Object[] {new InterMineId(22), new InterMineId(212)});
            batch.addRow(con, "table1", new InterMineId(32), colNames, new Object[] {new InterMineId(32), new InterMineId(312)});
            batch.flush(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new InterMineId(12), new InterMineId(112));
            expected.put(new InterMineId(22), new InterMineId(212));
            expected.put(new InterMineId(32), new InterMineId(312));
            expected.put(new InterMineId(13), new InterMineId(103));
            expected.put(new InterMineId(23), new InterMineId(203));
            expected.put(new InterMineId(33), new InterMineId(303));
            expected.put(new InterMineId(15), new InterMineId(105));
            expected.put(new InterMineId(25), new InterMineId(205));
            expected.put(new InterMineId(35), new InterMineId(305));
            assertEquals(expected, got);
            batch.addRow(con, "table1", new InterMineId(42), colNames, new Object[] {new InterMineId(42), new InterMineId(402)});
            batch.addRow(con, "table1", new InterMineId(52), colNames, new Object[] {new InterMineId(52), new InterMineId(502)});
            batch.addRow(con, "table1", new InterMineId(53), colNames, new Object[] {new InterMineId(53), new InterMineId(503)});
            batch.addRow(con, "table1", new InterMineId(55), colNames, new Object[] {new InterMineId(55), new InterMineId(505)});
            batch.deleteRow(con, "table1", "col1", new InterMineId(12));
            batch.deleteRow(con, "table1", "col1", new InterMineId(13));
            batch.deleteRow(con, "table1", "col1", new InterMineId(15));
            batch.deleteRow(con, "table1", "col1", new InterMineId(22));
            batch.deleteRow(con, "table1", "col1", new InterMineId(23));
            batch.deleteRow(con, "table1", "col1", new InterMineId(25));
            batch.deleteRow(con, "table1", "col1", new InterMineId(42));
            batch.deleteRow(con, "table1", "col1", new InterMineId(43));
            batch.deleteRow(con, "table1", "col1", new InterMineId(45));
            batch.addRow(con, "table1", new InterMineId(22), colNames, new Object[] {new InterMineId(22), new InterMineId(222)});
            batch.addRow(con, "table1", new InterMineId(23), colNames, new Object[] {new InterMineId(23), new InterMineId(223)});
            batch.addRow(con, "table1", new InterMineId(25), colNames, new Object[] {new InterMineId(25), new InterMineId(225)});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            r = s.executeQuery("SELECT col1, col2 FROM table1");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            expected = new TreeMap();
            expected.put(new InterMineId(22), new InterMineId(222));
            expected.put(new InterMineId(23), new InterMineId(223));
            expected.put(new InterMineId(25), new InterMineId(225));
            expected.put(new InterMineId(32), new InterMineId(312));
            expected.put(new InterMineId(33), new InterMineId(303));
            expected.put(new InterMineId(35), new InterMineId(305));
            expected.put(new InterMineId(52), new InterMineId(502));
            expected.put(new InterMineId(53), new InterMineId(503));
            expected.put(new InterMineId(55), new InterMineId(505));
            assertEquals(expected, got);
            s = con.createStatement();
            r = s.executeQuery("SELECT col1, col2 FROM table1");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testInsertOnly() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(2), new InterMineId(202)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(3), new InterMineId(203)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(4), new InterMineId(204)});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new InterMineId(1), new InterMineId(201));
            expected.put(new InterMineId(2), new InterMineId(202));
            expected.put(new InterMineId(3), new InterMineId(203));
            expected.put(new InterMineId(4), new InterMineId(204));
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testDeleteOnly() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.addBatch("INSERT INTO table1 VALUES (2, 202)");
            s.addBatch("INSERT INTO table1 VALUES (3, 203)");
            s.addBatch("INSERT INTO table1 VALUES (4, 204)");
            s.addBatch("INSERT INTO table1 VALUES (5, 205)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.deleteRow(con, "table1", "col1", new InterMineId(2));
            batch.deleteRow(con, "table1", "col1", new InterMineId(4));
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table1");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new InterMineId(1), new InterMineId(201));
            expected.put(new InterMineId(3), new InterMineId(203));
            expected.put(new InterMineId(5), new InterMineId(205));
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testTypes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key int, int2 smallint, int4 int, int8 bigint, float real, double float, bool boolean, bigdecimal numeric, string text)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"key", "int2", "int4", "int8", "float", "double", "bool", "bigdecimal", "string"};
            batch.addRow(con, "table1", new InterMineId(1), colNames,
                    new Object[] {new InterMineId(1),
                        new Short((short) 45),
                        new InterMineId(765234),
                        new Long(86523876513242L),
                        new Float(5.45),
                        new Double(7632.234134),
                        Boolean.TRUE,
                        new BigDecimal("982413415465245.87639871238764321"),
                        "kjhlasdurhe"});
            batch.addRow(con, "table1", new InterMineId(2), colNames,
                    new Object[] {new InterMineId(2),
                        new Short((short) 0),
                        new InterMineId(0),
                        new Long(0L),
                        new Float(0.0),
                        new Double(0.0),
                        Boolean.FALSE,
                        new BigDecimal("0.0"),
                        "turnip"});
            batch.addRow(con, "table1", new InterMineId(3), colNames,
                    new Object[] {new InterMineId(3),
                        new Short((short) -1),
                        new InterMineId(-12342),
                        new Long(-12465432646L),
                        new Float(-5.4),
                        new Double(-234.342),
                        Boolean.FALSE,
                        new BigDecimal("0"),
                        "blooglark"});
            batch.addRow(con, "table1", new InterMineId(4), colNames,
                    new Object[] {new InterMineId(4),
                        new Short((short) -6),
                        new InterMineId(-54321),
                        new Long(-98765432198L),
                        new Float(-5.4321),
                        new Double(-543.21),
                        Boolean.TRUE,
                        new BigDecimal("0.000000"),
                        "wmd"});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT key, int2, int4, int8, float, double, bool, bigdecimal, string FROM table1");
            Map got = new TreeMap();
            StringBuilder message = new StringBuilder();
            while (r.next()) {
                for (int i = 1; i <= 8; i++) {
                    InterMineId key = new InterMineId(r.getInt(1) * 10 + i);
                    Object value = r.getObject(i + 1);
                    got.put(key, value);
                    message.append(key + "=(" + value.getClass().getName() + ", " + value + "), ");
                }
            }
            Map expected = new TreeMap();
            expected.put(new InterMineId(11), new InterMineId((short) 45));
            expected.put(new InterMineId(12), new InterMineId(765234));
            expected.put(new InterMineId(13), new Long(86523876513242L));
            expected.put(new InterMineId(14), new Float(5.45));
            expected.put(new InterMineId(15), new Double(7632.234134));
            expected.put(new InterMineId(16), Boolean.TRUE);
            expected.put(new InterMineId(17), new BigDecimal("982413415465245.87639871238764321"));
            expected.put(new InterMineId(18), "kjhlasdurhe");
            expected.put(new InterMineId(21), new InterMineId((short) 0));
            expected.put(new InterMineId(22), new InterMineId(0));
            expected.put(new InterMineId(23), new Long(0L));
            expected.put(new InterMineId(24), new Float(0.0));
            expected.put(new InterMineId(25), new Double(0.0));
            expected.put(new InterMineId(26), Boolean.FALSE);
            expected.put(new InterMineId(27), new BigDecimal("0.0"));
            expected.put(new InterMineId(28), "turnip");
            expected.put(new InterMineId(31), new InterMineId((short) -1));
            expected.put(new InterMineId(32), new InterMineId(-12342));
            expected.put(new InterMineId(33), new Long(-12465432646L));
            expected.put(new InterMineId(34), new Float(-5.4));
            expected.put(new InterMineId(35), new Double(-234.342));
            expected.put(new InterMineId(36), Boolean.FALSE);
            expected.put(new InterMineId(37), new BigDecimal("0"));
            expected.put(new InterMineId(38), "blooglark");
            expected.put(new InterMineId(41), new InterMineId((short) -6));
            expected.put(new InterMineId(42), new InterMineId(-54321));
            expected.put(new InterMineId(43), new Long(-98765432198L));
            expected.put(new InterMineId(44), new Float(-5.4321));
            expected.put(new InterMineId(45), new Double(-543.21));
            expected.put(new InterMineId(46), Boolean.TRUE);
            expected.put(new InterMineId(47), new BigDecimal("0.000000"));
            expected.put(new InterMineId(48), "wmd");
            assertEquals(message.toString(), expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    /*
     * This test no longer works because we throttle analyses to only happen every ten minutes at most.
     *
    public void testPerformanceAndAnalyse() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key int, int4 int)");
            s.addBatch("CREATE INDEX table1_key ON table1(key)");
            s.executeBatch();
            con.commit();
            s = null;
            long start = System.currentTimeMillis();
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String[] colNames = new String[] {"key", "int4"};
            for (int i = 0; i < 100000; i++) {
                batch.addRow(con, "table1", new InterMineId(i), colNames, new Object[] {new InterMineId(i), new InterMineId(765234 * i)});
                if (i % 10000 == 9999) {
                    batch.flush(con);
                }
            }
            batch.flush(con);
            con.commit();
            System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to insert 100000 rows");
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT reltuples FROM pg_class WHERE relname = 'table1'");
            assertTrue(r.next());
            assertTrue("Expected rows to be > 60000 and < 101000 - was " + r.getInt(1),
                    r.getInt(1) > 60000 && r.getInt(1) < 101000);
            assertFalse(r.next());
            start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                if (i % 5 != 0) {
                    batch.deleteRow(con, "table1", "key", new InterMineId(i));
                }
                //if (i % 10000 == 9999) {
                //    batch.flush(con);
                //}
            }
            batch.close(con);
            con.commit();
            System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to delete 80000 rows");
            r = s.executeQuery("SELECT reltuples FROM pg_class WHERE relname = 'table1'");
            assertTrue(r.next());
            assertTrue("Expected rows to be > 19000 and < 35000 - was " + r.getInt(1),
                    r.getInt(1) > 19000 && r.getInt(1) < 35000);
            assertFalse(r.next());
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
//*/

    public void testUTF() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key text)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.addRow(con, "table1", null, new String[] {"key"}, new Object[] {"Flibble\u00A0fds\u786f"});
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT key FROM table1");
            assertTrue(r.next());
            assertEquals("Flibble\u00A0fds\u786f", r.getString(1));
            assertFalse(r.next());
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testIndirections() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            expected.add(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            expected.remove(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.flush(con);
            con.commit();
            expected.add(new Row(1, 2));
            assertEquals(expected, getGot(con));

            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 2);
            batch.addRow(con, "table1", "a", "b", 1, 3);
            batch.flush(con);
            con.commit();
            expected.add(new Row(1, 2));
            expected.add(new Row(1, 3));
            assertEquals(expected, getGot(con));

            batch.deleteRow(con, "table1", "a", "b", 1, 2);
            batch.close(con);
            con.commit();
            expected.remove(new Row(1, 2));
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testManyDeletes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.addBatch("CREATE INDEX table1_key on table1(a, b)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"a", "b"};
            for (int i = 0; i < 10000; i++) {
                batch.addRow(con, "table1", new InterMineId(i), colNames, new Object[] {new InterMineId(i), new InterMineId(i * 2876123)});
            }
            batch.flush(con);
            con.commit();
            con.createStatement().execute("ANALYSE");
            for (int i = 0; i < 10000; i++) {
                batch.deleteRow(con, "table1", "a", new InterMineId(i));
            }
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testManyIndirectionDeletes() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(a int, b int)");
            s.addBatch("CREATE INDEX table1_key on table1(a, b)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            for (int i = 0; i < 10000; i++) {
                batch.addRow(con, "table1", "a", "b", i, i * 2876123);
            }
            batch.flush(con);
            con.commit();
            con.createStatement().execute("ANALYSE");
            for (int i = 0; i < 10000; i++) {
                batch.deleteRow(con, "table1", "a", "b", i, i * 2876123);
            }
            batch.flush(con);
            con.commit();
            Set expected = new HashSet();
            assertEquals(expected, getGot(con));
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

    public void testPartialFlush() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            try {
                s.execute("DROP TABLE table2");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 int)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.addBatch("CREATE TABLE table2(col1 int, col2 int)");
            s.addBatch("INSERT INTO table2 VALUES (1, 201)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(2), new InterMineId(202)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(3), new InterMineId(203)});
            batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(4), new InterMineId(204)});
            batch.addRow(con, "table2", null, colNames, new Object[] {new InterMineId(2), new InterMineId(202)});
            batch.addRow(con, "table2", null, colNames, new Object[] {new InterMineId(3), new InterMineId(203)});
            batch.addRow(con, "table2", null, colNames, new Object[] {new InterMineId(4), new InterMineId(204)});
            batch.flush(con, Collections.singleton("table1"));
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT col1, col2 FROM table2");
            Map got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            Map expected = new TreeMap();
            expected.put(new InterMineId(1), new InterMineId(201));
            assertEquals(expected, got);
            r = s.executeQuery("SELECT col1, col2 FROM table1");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            expected.put(new InterMineId(2), new InterMineId(202));
            expected.put(new InterMineId(3), new InterMineId(203));
            expected.put(new InterMineId(4), new InterMineId(204));
            assertEquals(expected, got);
            batch.close(con);
            r = s.executeQuery("SELECT col1, col2 FROM table2");
            got = new TreeMap();
            while (r.next()) {
                got.put(r.getObject(1), r.getObject(2));
            }
            assertEquals(expected, got);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table2");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }

/*     --------This test works, and is important, but takes a huge amount of time to run.-------
    public void testPartialFlushAccounting() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            try {
                s.execute("DROP TABLE table2");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(col1 int, col2 text)");
            s.addBatch("INSERT INTO table1 VALUES (1, 201)");
            s.addBatch("CREATE TABLE table2(col1 int, col2 text)");
            s.addBatch("INSERT INTO table2 VALUES (1, 201)");
            s.executeBatch();
            con.commit();
            s = null;

            StringBuffer longBuffer = new StringBuffer(1000000);
            for (int i = 0; i < 19000; i++) {
                longBuffer.append("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
            }
            String longString = longBuffer.toString();
            longBuffer = null;

            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String colNames[] = new String[] {"col1", "col2"};
            for (int i = 0; i < 2000; i++) { // Write ~2GB to table1. Hope it doesn't run out of memory due to forgetting to flush table1.
                batch.addRow(con, "table1", null, colNames, new Object[] {new InterMineId(i), new String(longString)});
                batch.addRow(con, "table2", null, colNames, new Object[] {new InterMineId(i), new String("Hello" + i)});
                batch.flush(con, Collections.singleton("table2"));
            }
            con.commit();
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table2");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
*/
    private Set getGot(Connection con) throws SQLException {
        Statement s = con.createStatement();
        ResultSet r = s.executeQuery("SELECT a, b FROM table1");
        Set set = new HashSet();
        while (r.next()) {
            set.add(new Row(r.getInt(1), r.getInt(2)));
        }
        return set;
    }

    public abstract BatchWriter getWriter();
    public int getThreshold() {
        return InterMineId.MAX_VALUE;
    }
}
