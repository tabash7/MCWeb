package org.cloudbus.mcweb.rules;

import org.assertj.core.util.Preconditions;

/**
 * A rule fact, denoting that a user is being denied access to a data centre.
 * 
 * @author nikolay.grozev
 *
 */
public class AdmissionDenied {
    
    private String userId;
    
    private String description;
    
    public AdmissionDenied(String userId, String description) {
        super();
        this.userId = userId;
        this.description = description;
    }

    /**
     * Returns the id of the user.
     * @return the id of the user.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the id of the user.
     * @param userId - the id of the user. Must not be null.
     */
    public void setUserId(String userId) {
        Preconditions.checkNotNull(userId);
        this.userId = userId;
    }

    /**
     * Returns a readable description of why the admission was denied.
     * @return a readable description of why the admission was denied.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the readable description of why the admission was denied.
     * @param description - readable description of why the admission was denied. Must not be null.
     */
    public void setDescription(String description) {
        Preconditions.checkNotNull(description);
        this.description = description;
    }
}
