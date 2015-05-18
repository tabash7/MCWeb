package org.cloudbus.mcweb.rules;


public interface IRelation<T> {
    public T getLhs();

    public void setLhs(T lhs);

    public T getRhs();

    public void setRhs(T rhs);

}
