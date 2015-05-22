package org.cloudbus.mcweb.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.util.Preconditions;

/**
 * An end-user of the system.
 * 
 * @author nikolay.grozev
 *
 */
public class User {

    private String userId;
    private Set<String> citizenships = new HashSet<>();
    private Set<String> tags = new HashSet<>();

    /**
     * Ctor.
     * @param userId - the user id. Must not be null.
     * @param citizenships - the list of citizenships. Must not be null;
     * @param tags - the list of tags. Must not be null;
     */
    public User(final String userId, final Set<String> citizenships, final Set<String> tags) {
        setUserId(userId);
        setCitizenships(citizenships);
        setTags(tags);
    }

    /**
     * Returns the user id.
     * @return the id of the user.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user id.
     * @param userId - the new user id. Must not be null;
     */
    public void setUserId(String userId) {
        Preconditions.checkNotNull(userId);
        this.userId = userId;
    }

    /**
     * Returns the citizenships of this user.
     * @return the citizenships of this user.
     */
    public Set<String> getCitizenships() {
        return citizenships;
    }

    /**
     * Sets the citizenships of this user.
     * @param citizenships - the citizenships of this user. Must not be null.
     */
    public void setCitizenships(Set<String> citizenships) {
        Preconditions.checkNotNull(citizenships);
        this.citizenships = citizenships;
    }

    /**
     * Returns the tags associated with this user.
     * @return the tags associated with this user.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags of the user.
     * @param tags - the tags of the user. Must not be null.
     */
    public void setTags(Set<String> tags) {
        Preconditions.checkNotNull(tags);
        this.tags = tags;
    }
    
    @Override
    public String toString() {
        return String.format("User: id=%s, citizenships=%s, tags=%s", userId,
                Arrays.toString(citizenships.toArray()),
                Arrays.toString(tags.toArray()));
    }
}
