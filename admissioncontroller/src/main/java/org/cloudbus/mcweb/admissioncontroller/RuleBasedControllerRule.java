package org.cloudbus.mcweb.admissioncontroller;

import java.util.Set;

import org.cloudbus.mcweb.rules.DataCentre;
import org.cloudbus.mcweb.rules.RuleEngine;
import org.cloudbus.mcweb.rules.User;

import com.google.common.base.Preconditions;


/**
 * An admission controller which uses a rule inference engine. 
 * 
 * @author nikolay.grozev
 *
 */
public class RuleBasedControllerRule implements IAdmissionControllerRule {

    @Override
    public void close() throws Exception {
        RuleEngine.dispose();
    }

    @Override
    public boolean isEligible(final User user, final DataCentre dataCentre) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(dataCentre);
        
        Set<String> nonApplicable = RuleEngine.determineUsersApplicability(dataCentre, user);
        
        return !nonApplicable.contains(user.getUserId());
    }

    @Override
    public boolean backOff() {
        // TODO Auto-generated method stub
        return false;
    }
}
