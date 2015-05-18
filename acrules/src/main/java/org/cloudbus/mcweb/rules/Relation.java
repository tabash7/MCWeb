package org.cloudbus.mcweb.rules;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.assertj.core.util.Preconditions;


public abstract class Relation<T> implements IRelation<T> {
    
    protected static Logger LOG = Logger.getLogger(Relation.class.getCanonicalName());
    
    protected T lhs;
    protected T rhs;
            
    public Relation() {
    }

    public Relation(final T lhs, final T rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public T getLhs() {
        return lhs;
    }

    public void setLhs(T lhs) {
        this.lhs = lhs;
    }

    public T getRhs() {
        return rhs;
    }

    public void setRhs(T rhs) {
        this.rhs = rhs;
    }

    public static IRelation<?> newInstance(Class<? extends IRelation<?>> clazz, Object lhs, Object rhs) {
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(lhs);
        Preconditions.checkNotNull(rhs);
        
        try {
            IRelation<Object> relation = (IRelation<Object>)clazz.newInstance();
            relation.setLhs(lhs);
            relation.setLhs(rhs);
            return relation;
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Could not create relation", e);
            throw new IllegalStateException(e);
        }
    }
    
}