package org.intermine.webservice.server.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.intermine.model.InterMineId;
import org.junit.Test;

public class DisjointListTest {

    @Test
    public void testCreate() {
        DisjointList<InterMineId, String> numsAndStrings =
                new DisjointList<InterMineId, String>();
        numsAndStrings.addLeft(1);
        numsAndStrings.addRight("two");
        numsAndStrings.addLeft(3);
        numsAndStrings.addRight("four");
        assertEquals(numsAndStrings.size(), 4);
        List<Either<InterMineId, String>> expected = Arrays.asList(
                new Either.Left<InterMineId, String>(1),
                new Either.Right<InterMineId, String>("two"),
                new Either.Left<InterMineId, String>(3),
                new Either.Right<InterMineId, String>("four"));
        assertEquals(expected, numsAndStrings);
        Collections.reverse(numsAndStrings);
        assertTrue(!expected.equals(numsAndStrings));
    }

}
