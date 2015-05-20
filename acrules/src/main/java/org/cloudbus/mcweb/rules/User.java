package org.cloudbus.mcweb.rules;

import java.util.HashSet;
import java.util.Set;

/**
 * An end-user.
 * 
 * @author nikolay.grozev
 *
 */
public class User {

    private String userId;
    
    private Set<String> citizenships = new HashSet<>();
    
    private Set<String> tags = new HashSet<>();

    
    public User(String userId, Set<String> citizenships, Set<String> tags) {
        super();
        this.userId = userId;
        this.citizenships = citizenships;
        this.tags = tags;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getCitizenships() {
        return citizenships;
    }

    public void setCitizenships(Set<String> citizenships) {
        this.citizenships = citizenships;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
