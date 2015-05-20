package org.cloudbus.mcweb.rules;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * 
 * @author nikolay.grozev
 *
 */
public class DataCentre {

    private String locationCode;
    
    private Set<String> tags = new HashSet<>();

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
