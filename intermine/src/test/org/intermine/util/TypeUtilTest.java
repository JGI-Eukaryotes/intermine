/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.util;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flymine.model.testmodel.*;

public class TypeUtilTest extends TestCase
{
    public TypeUtilTest(String arg) {
        super(arg);
    }

    public void testInvalidGetField() {
        Field f = TypeUtil.getField(Manager.class, "FullTime");
        assertNull("Field should be null", f);
    }

    public void testValidNonInheritedGetField() {
        Field f = TypeUtil.getField(Manager.class, "title");
        assertNotNull("Field should not be null", f);
    }

    public void testValidInheritedGetField() {
        Field f = TypeUtil.getField(Manager.class, "fullTime");
        assertNotNull("Field should not be null", f);
    }

    public void testGetFieldValue() throws Exception {
        assertNotNull(TypeUtil.getFieldValue(new Company(), "departments"));
    }

    public void testSetFieldValue() throws Exception {
        Manager m = new Manager();
        String fieldValue = "Accountant";
        TypeUtil.setFieldValue(m, "title", fieldValue);
        assertEquals(fieldValue, m.getTitle());
    }

    public void testGetElementTypeNull() throws Exception {
        Collection c = null;
        try {
            TypeUtil.getElementType(c);
            fail("Expected NullPointerException");
        } catch (RuntimeException e) {
        }
    }

    public void testGetElementTypeEmpty() throws Exception {
        Collection c = new ArrayList();
        try {
            TypeUtil.getElementType(c);
            fail("Expected NoSuchElementException");
        } catch (RuntimeException e) {
        }
    }

    public void testGetElementType() throws Exception {
        Collection c = new ArrayList();
        c.add(new String());
        assertEquals(String.class, TypeUtil.getElementType(c));
    }

    public void testGetFieldToGetter() throws Exception {
        Map expected = new HashMap();
        Class c = Address.class;
        Field f1 = c.getDeclaredField("address");
        Method m1 = c.getMethod("getAddress", new Class[] {});
        expected.put(f1, m1);

        assertEquals(expected, TypeUtil.getFieldToGetter(c));
        assertEquals(expected, TypeUtil.getFieldToGetter(c));
    }

    public void testGetFieldToGetterNoGetters() throws Exception {
        Map expected = new HashMap();
        Class c = NoGetSet.class;  // random class with no getters
        try {
            TypeUtil.getFieldToGetter(c);
        } catch (NullPointerException e) {
            fail("Should not have thrown a NullPointerException");
        }

    }

    public void testGetFieldToSetter() throws Exception {
        Map expected = new HashMap();
        Class c = Address.class;
        Field f1 = c.getDeclaredField("address");
        Method m1 = c.getMethod("setAddress", new Class[] {String.class});
        expected.put(f1, m1);

        assertEquals(expected, TypeUtil.getFieldToSetter(c));
        assertEquals(expected, TypeUtil.getFieldToSetter(c));
    }

    public void testGetFieldToSetterNoSetters() throws Exception {
        Map expected = new HashMap();
        Class c = NoGetSet.class;  // random class with no setters
        try {
            TypeUtil.getFieldToSetter(c);
        } catch (NullPointerException e) {
            fail("Should not have thrown a NullPointerException");
        }

    }

    public void testPackageName() throws Exception {
        assertEquals("", TypeUtil.packageName("test"));
        assertEquals("package", TypeUtil.packageName("package.test"));
    }

    public void testUnqualifiedName() throws Exception {
        assertEquals("test", TypeUtil.unqualifiedName("test"));
        assertEquals("test", TypeUtil.unqualifiedName("package.test"));
    }

    //===========================

    private class NoGetSet {
    }


}
