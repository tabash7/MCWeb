package org.cloudbus.mcweb.entrypoint;

import org.cloudbus.mcweb.AdmissionControllerResponse;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Response received from the admission controller, after an enquiry for a given
 * user.
 * 
 * @author nikolay.grozev
 *
 */
public class EPAdmissionControllerResponse extends AdmissionControllerResponse {

    private final CloudSite site;

    /**
     * Constr.
     * 
     * @param userToken
     *            - see superclass.
     * @param eligible
     *            - see superclass.
     * @param costEstimation
     *            - see superclass.
     * @param site
     *            - the cloud site, whose response this is. Must not be null.
     */
    public EPAdmissionControllerResponse(final String userToken, final boolean eligible, final double costEstimation, final CloudSite site) {
        super(userToken, eligible, costEstimation);
        Preconditions.checkNotNull(site);

        this.site = site;
    }
    
    
    public EPAdmissionControllerResponse(final AdmissionControllerResponse response, final CloudSite site) {
        this(response.getUserToken(), response.isEligible(), response.getCostEstimation(), site);
    }

    /**
     * Returns the cloud site, whose response this is.
     * 
     * @return the cloud site, whose response this is.
     */
    public CloudSite getCloudSite() {
        return site;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("CloudSite", this.site.getName())
                .add("User", this.getUserToken())
                .add("Cost est", this.getCostEstimation())
                .add("Eligible", this.isEligible())
                .toString();
    }
}
