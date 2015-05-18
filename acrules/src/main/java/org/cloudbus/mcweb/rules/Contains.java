package org.cloudbus.mcweb.rules;

public class Contains extends Relation<String> implements IReflexiveRelation<String>, ITransitiveRelation<String> {

    public Contains() {
        super();
    }

    public Contains(final String lhs, final String rhs) {
        super(lhs, rhs);
    }
}
