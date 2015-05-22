package org.cloudbus.mcweb.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.util.Preconditions;

/**
 * A cloud data centre.
 * 
 * @author nikolay.grozev
 *
 */
public class DataCentre {

    private String locationCode;
    private String providerCode;
    private Set<String> tags = new HashSet<>();

    /**
     * Ctor.
     * @param locationCode - the location code. Must not be null.
     * @param providerCode - the provider code. Must not be null.
     * @param tags - the tags code. Must not be null.
     */
    public DataCentre(final String locationCode, final String providerCode, final Set<String> tags) {
        setLocationCode(locationCode);
        setProviderCode(providerCode);
        setTags(tags);
    }

    /**
     * Returns the location code of the DC.
     * @return the location code of the DC.
     */
    public String getLocationCode() {
        return locationCode;
    }

    /**
     * Sets the location code of the DC.
     * @param locationCode - the new location code. Must not be null.
     */
    public void setLocationCode(final String locationCode) {
        Preconditions.checkNotNull(locationCode);
        this.locationCode = locationCode;
    }

    /**
     * Returns the code of the cloud provider.
     * @return the code of the cloud provider.
     */
    public String getProviderCode() {
        return providerCode;
    }

    /**
     * Sets the code of the cloud provider.
     * @param providerCode - the cloud provider's code. Must not be null.
     */
    public void setProviderCode(String providerCode) {
        Preconditions.checkNotNull(providerCode);
        this.providerCode = providerCode;
    }

    /**
     * Returns the tags.
     * @return the tags.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags of this cloud data centre.
     * @param tags - the tags. Must not be null.
     */
    public void setTags(Set<String> tags) {
        Preconditions.checkNotNull(tags);
        this.tags = tags;
    }
    
    @Override
    public String toString() {
        return String.format("DC: locationCode=%s, providerCode=%s, tags=%s", locationCode,
                providerCode,
                Arrays.toString(tags.toArray()));
    }
}
