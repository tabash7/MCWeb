package org.cloudbus.mcweb.rules.relations;

/**
 * Represents an algebraic relation with a left hand side (lhs) and
 * a rigth hand side (rhs);
 * 
 * @author nikolay.grozev
 *
 * @param <T> - the compile time type of the arguements.
 */
public interface IRelation<T> {
    
    /**
     * Returns the left hand side (lhs) of the relation.
     * @return the left hand side (lhs) of the relation.
     */
    public T getLhs();

    /**
     * Sets the left hand side (lhs) of the relation.
     * @param lhs - the left hand side (lhs) of the relation. Must not be null. Must of the type specified by {@link IRelation.getElementType}.
     */
    public void setLhs(T lhs);

    /**
     * Returns the right hand side (lhs) of the relation.
     * @return the right hand side (lhs) of the relation.
     */
    public T getRhs();
    
    /**
     * Sets the right hand side (rhs) of the relation.
     * @param rhs - the right hand side (rhs) of the relation. Must not be null. Must of the type specified by {@link IRelation.getElementType}.
     */
    public void setRhs(T rhs);
    
    /**
     * Returns the expected type of the elements of this relation. Used for validation, because of type erasure in Java.
     * @return the expected type of the elements of this relation. 
     */
    public Class<? extends T> getElementType();
    
    /**
     * Sets the expected type of the elements of this relation. Used for validation, because of type erasure in Java.
     * @param elementType - the expected type of the elements of this relation. Used for validation. Must not be null.
     * Must match or be a supertype of the types of lhs and rhs.
     */
    public void setElementType(Class<? extends T> elementType);
    
    /**
     * Returns if this relation is applicable to the specified object.
     * @param o - the object to check for.
     * @return if this relation is applicable to the specified object.
     */
    public default boolean isApplicableTo(Object o){
        return o != null && getElementType().isInstance(o);
    }
}
