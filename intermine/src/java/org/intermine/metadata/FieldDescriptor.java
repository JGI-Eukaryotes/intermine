/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.metadata;

/**
 * Abstract representation of a field within a class - could be an attribute, an
 * object reference or a collection.
 *
 * @author Richard Smith
 */
public abstract class FieldDescriptor
{
    /**
     * Not a relationship between objects
     */
    public static final int NOT_RELATION = 0;

    /**
     * A 1:1 relationship
     */
    public static final int ONE_ONE_RELATION = 1;

    /**
     * A 1:N relationship.
     */
    public static final int ONE_N_RELATION = 2;

    /**
     * A N:1 relationship.
     */
    public static final int N_ONE_RELATION = 3;

    /**
     * A M:N relationship.
     */
    public static final int M_N_RELATION = 4;

    protected final String name; // name of field
    protected final boolean primaryKey;
    protected ClassDescriptor cld; // parent class of this field
    private boolean cldSet = false;

    /**
     * Construct, name of field must not be null
     * @param name name of field in class
     * @param primaryKey true if part of the class' primary key
     * @throws IllegalArgumentException if name argument is null
     */
    public FieldDescriptor(String name, boolean primaryKey)
        throws IllegalArgumentException {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
        this.primaryKey = primaryKey;
    }

    /**
     * Get the ClassDescriptor for this field's class.
     * @return a the ClassDescriptor for this field's class
     */
    public ClassDescriptor getClassDescriptor() {
        return this.cld;
    }

    /**
     * Get the name of the described field.
     * @return name of the field
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns true if this fields makes up part of the Class' primary key
     * @return true if part of primary key
     */
    public boolean isPrimaryKey() {
        return this.primaryKey;
    }

    /**
     * Set the parent ClassDescriptor - should be called when this is added to
     * a ClassDescriptor.  ClassDescriptor in this class is final so a MetadataException
     * is thrown if method called again.
     * @param cld the parent ClassDescriptor
     * @throws IllegalStateException if the parent ClassDescriptor is not set
     */
     protected void setClassDescriptor(ClassDescriptor cld) throws IllegalStateException {
         if (cld == null) {
             throw new NullPointerException("cld cannot be null");
         }
         if (cldSet) {
            throw new IllegalStateException("ClassDescriptor has already been set and "
                                            + "may not be changed.");
        }
        this.cld = cld;
        cldSet = true;
     }

    /**
     * Return an integer describing the type of relationship this field represents,
     * where relationship types are 1:1, 1:N, N:1, M:N and "not a relationship".
     *
     * @return int to describe the relationship type
     */
    public abstract int relationType(); // attr (NOT_RELATION), ref (N_1, 1_1) or coll (1_N, M_N)
}
