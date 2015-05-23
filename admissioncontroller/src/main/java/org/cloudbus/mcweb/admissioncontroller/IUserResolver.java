package org.cloudbus.mcweb.admissioncontroller;

import org.cloudbus.mcweb.rules.User;

/**
 * 
 * @author nikolay.grozev
 *
 */
public interface IUserResolver {

    /**
     * Resolves the user metadata based on the hise/her id.
     * @param userId - the id of the user. Must not be null.
     * @return the user corresponding to this id, or null, if no such user could be found.
     */
    public User resolve(final String userId);
    
}
