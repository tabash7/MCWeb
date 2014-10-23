package org.cloudbus.mcweb.admissioncontroller;

/**
 * Represents application specific admission control rules.
 * 
 * @author nikolay.grozev
 */
public interface IAdmissionControllerRule extends AutoCloseable {

    /**
     * Returns, whether the user is eligible in this cloud site.
     * 
     * @param userToken
     *            - an identifier of the user. Must not be null.
     * @return whether the user is eligible in this cloud site.
     */
    public boolean isEligible(final String userToken);

    /**
     * Returns, whether the admission controller should signal the entry point
     * back-off from this cloud site.
     * 
     * @return whether the admission controller should signal the entry point
     *         back-off.
     */
    public boolean backOff();
}
