package org.cloudbus.mcweb.rules.relations;

import java.util.logging.Logger;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Preconditions;


/**
 * A base relation implementation. 
 * 
 * @author nikolay.grozev
 *
 * @param <T> - the type of the relations implementation.
 */
public abstract class Relation<T> implements IRelation<T> {
    
    /** Logger. */
    protected static Logger LOG = Logger.getLogger(Relation.class.getCanonicalName());
    
    private T lhs;
    private T rhs;
    private Class<? extends T> elementType;
            
    /**
     * Default ctor.
     */
    protected Relation() {
    }

    /**
     * Ctor.
     * @param elementType - the type of the relation's elements (needed for validation because of type erasure). Must not be null; 
     * @param lhs - the LHS. Must not be null. Must be of the specified type.
     * @param rhs - the RHS. Must not be null. Must be of the specified type.
     */
    public Relation(Class<? extends T> elementType, final T lhs, final T rhs) {
        setElementType(elementType);
        setLhs(lhs);
        setRhs(rhs);
    }

    /** {@inheritDoc} */
    @Override
    public T getLhs() {
        return lhs;
    }

    /** {@inheritDoc} */
    @Override
    public void setLhs(T lhs) {
        Preconditions.checkNotNull(lhs);
        Assertions.assertThat(getElementType() == null || getElementType().isInstance(lhs));
        this.lhs = lhs;
    }

    /** {@inheritDoc} */
    @Override
    public T getRhs() {
        return rhs;
    }

    /** {@inheritDoc} */
    @Override
    public void setRhs(T rhs) {
        Preconditions.checkNotNull(rhs);
        Assertions.assertThat(getElementType() == null || getElementType().isInstance(rhs));
        this.rhs = rhs;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends T> getElementType() {
        return elementType;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setElementType(Class<? extends T> elementType) {
        Preconditions.checkNotNull(elementType);
        Assertions.assertThat(elementType.isInstance(lhs));
        Assertions.assertThat(elementType.isInstance(rhs));
        this.elementType = elementType;
    }
}