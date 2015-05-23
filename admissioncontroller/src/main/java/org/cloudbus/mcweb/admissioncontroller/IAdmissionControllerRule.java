package org.cloudbus.mcweb.admissioncontroller;

import org.cloudbus.mcweb.rules.DataCentre;
import org.cloudbus.mcweb.rules.User;

/**
 * Represents application specific admission control rules.
 * 
 * @author nikolay.grozev
 */
public interface IAdmissionControllerRule extends AutoCloseable {

    /**
     * Returns, whether the user is eligible in this cloud site.
     * 
     * @param user - the user to check for. Must not be null.
     * @param dataCenre - the DC to check for. Must not be null.
     * 
     * @return whether the user is eligible in this cloud site.
     */
    public boolean isEligible(User user, DataCentre dataCentre);

    /**
     * Returns, whether the admission controller should signal the entry point
     * to back-off from this cloud site.
     * 
     * @return whether the admission controller should signal the entry point
     *         back-off.
     */
    public boolean backOff();
}
