package org.cloudbus.mcweb.rules.relations;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.assertj.core.util.Preconditions;

/**
 * A utility for working with relations.
 * 
 * @author nikolay.grozev
 */
public class Relations {

    /** Logger. */
    protected static Logger LOG = Logger.getLogger(Relations.class.getCanonicalName());
    
    /**
     * Creates a new relation with the specified element type, LHS and RHS.
     * @param relationType - the relations type. Must not be null.
     * @param elementType - the element type. Must not be null. Must match or be a supertype of the actual types of LHS and RHS.
     * @param lhs - the left hand side. Must not be null.
     * @param rhs - the right hand side. Must not be null.
     * @return a relation of the given type with the specified element type, LHS and RHS.
     */
    public static IRelation<?> newInstance(Class<? extends IRelation<?>> relationType, Class<?> elementType, Object lhs, Object rhs) {
        Preconditions.checkNotNull(elementType);
        Preconditions.checkNotNull(lhs);
        Preconditions.checkNotNull(rhs);
        
        try {
            @SuppressWarnings("unchecked")
            IRelation<Object> relation = (IRelation<Object>)relationType.newInstance();
            relation.setLhs(lhs);
            relation.setRhs(rhs);
            relation.setElementType(elementType);
            return relation;
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Could not create relation", e);
            throw new IllegalStateException(e);
        }
    }
}
