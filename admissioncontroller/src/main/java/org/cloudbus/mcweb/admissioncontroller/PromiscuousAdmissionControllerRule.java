package org.cloudbus.mcweb.admissioncontroller;

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
    public boolean isEligible(String userToken) {
        return true;
    }
    
    @Override
    public boolean backOff() {
        return false;
    }
}
