package org.cloudbus.mcweb.rules;

import org.cloudbus.mcweb.rules.relations.IReflexiveRelation;
import org.cloudbus.mcweb.rules.relations.ITransitiveRelation;
import org.cloudbus.mcweb.rules.relations.Relation;

/**
 * A reflexive and transitive relation expressing containment of jurisdictions
 * on some principle. Each jurisdiction is identified by a unique code.
 * 
 * @author nikolay.grozev
 *
 */
public class ContainsJurisdiction extends Relation<String> implements IReflexiveRelation<String>, ITransitiveRelation<String> {

    /**
     * Ctor.
     */
    public ContainsJurisdiction() {
        setElementType(String.class);
    }

    /**
     * Ctor.
     * @param lhs - the code of the first jurisdiction.
     * @param rhs - the code of the second jurisdiction.
     */
    public ContainsJurisdiction(final String lhs, final String rhs) {
        super(String.class, lhs, rhs);
    }
}
