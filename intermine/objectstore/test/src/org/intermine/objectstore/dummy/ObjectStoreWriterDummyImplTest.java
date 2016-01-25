package org.intermine.objectstore.dummy;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.util.DynamicUtil;

import java.util.Collections;
import java.util.Map;

import org.intermine.model.InterMineId;
import junit.framework.TestCase;

/**
 * Tests for the ObjectStoreWriterDummyImpl class.
 * @author Kim Rutherford
 */
public class ObjectStoreWriterDummyImplTest extends TestCase {
    public void testStore() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setModel(Model.getInstanceByName("testmodel"));
        ObjectStoreWriterDummyImpl osw = new ObjectStoreWriterDummyImpl(os);

        InterMineObject o1 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        osw.store(o1);

        InterMineObject o2 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        o2.setId(new InterMineId(1));
        osw.store(o2);

        InterMineObject o3 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        osw.store(o3);

        InterMineObject o4 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        o4.setId(new InterMineId(100));
        // store twice to make sure we get only one copy
        osw.store(o4);
        osw.store(o4);


        InterMineObject o5 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        // store twice to make sure we get only one copy
        osw.store(o5);
        osw.store(o5);

        Map storedObjects = osw.getStoredObjects();

        assertEquals(5, storedObjects.size());

        assertTrue(storedObjects.get(new InterMineId(0)) == o1);
        assertTrue(((Company) storedObjects.get(new InterMineId(0))).getId().equals(new InterMineId(0)));
        assertTrue(storedObjects.get(new InterMineId(1)) == o2);
        assertTrue(((Company) storedObjects.get(new InterMineId(1))).getId().equals(new InterMineId(1)));
        assertTrue(storedObjects.get(new InterMineId(2)) == o3);
        assertTrue(((Company) storedObjects.get(new InterMineId(2))).getId().equals(new InterMineId(2)));
        assertTrue(storedObjects.get(new InterMineId(3)) == o5);
        assertTrue(((Company) storedObjects.get(new InterMineId(3))).getId().equals(new InterMineId(3)));
        assertTrue(storedObjects.get(new InterMineId(100)) == o4);
        assertTrue(((Company) storedObjects.get(new InterMineId(100))).getId().equals(new InterMineId(100)));
    }

    public void testStoreTransaction() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setModel(Model.getInstanceByName("testmodel"));
        ObjectStoreWriterDummyImpl osw = new ObjectStoreWriterDummyImpl(os);

        osw.beginTransaction();

        InterMineObject o1 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        osw.store(o1);

        InterMineObject o2 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        o2.setId(new InterMineId(1));
        osw.store(o2);

        InterMineObject o3 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        osw.store(o3);

        assertTrue(osw.isInTransaction());

        osw.commitTransaction();

        assertFalse(osw.isInTransaction());

        Map storedObjects = osw.getStoredObjects();

        assertEquals(3, storedObjects.size());

        assertTrue(storedObjects.get(new InterMineId(0)) == o1);
        assertTrue(((Company) storedObjects.get(new InterMineId(0))).getId().equals(new InterMineId(0)));
        assertTrue(storedObjects.get(new InterMineId(1)) == o2);
        assertTrue(((Company) storedObjects.get(new InterMineId(1))).getId().equals(new InterMineId(1)));
        assertTrue(storedObjects.get(new InterMineId(2)) == o3);
        assertTrue(((Company) storedObjects.get(new InterMineId(2))).getId().equals(new InterMineId(2)));



        // an aborted transaction
        osw.beginTransaction();

        assertTrue(osw.isInTransaction());

        InterMineObject o4 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        o4.setId(new InterMineId(100));
        osw.store(o4);


        InterMineObject o5 =
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(Company.class));
        osw.store(o5);

        storedObjects = osw.getStoredObjects();

        assertEquals(5, storedObjects.size());

        osw.abortTransaction();

        assertFalse(osw.isInTransaction());

        storedObjects = osw.getStoredObjects();

        // check that we have the same objects as before


        assertEquals(3, storedObjects.size());

        assertTrue(storedObjects.get(new InterMineId(0)) == o1);
        assertTrue(((Company) storedObjects.get(new InterMineId(0))).getId().equals(new InterMineId(0)));
        assertTrue(storedObjects.get(new InterMineId(1)) == o2);
        assertTrue(((Company) storedObjects.get(new InterMineId(1))).getId().equals(new InterMineId(1)));
        assertTrue(storedObjects.get(new InterMineId(2)) == o3);
        assertTrue(((Company) storedObjects.get(new InterMineId(2))).getId().equals(new InterMineId(2)));
    }
}
