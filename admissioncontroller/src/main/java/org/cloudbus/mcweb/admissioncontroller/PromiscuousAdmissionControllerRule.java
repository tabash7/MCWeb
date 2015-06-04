package org.cloudbus.mcweb.admissioncontroller;

import org.cloudbus.mcweb.DataCentre;
import org.cloudbus.mcweb.User;

import com.google.common.base.Preconditions;

/**
 * 
 * An admission control rule, which accepts any user and never asks for a back-off. 
 * 
 * @author nikolay.grozev
 *
 */
public class PromiscuousAdmissionControllerRule implements IAdmissionControllerRule {

    @Override
    public void close() throws Exception {
        // pass
    }
    
    @Override
    public boolean isEligible(User user, DataCentre dataCentre) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(dataCentre);
        return true;
    }
    
    @Override
    public boolean backOff() {
        return false;
    }
}
