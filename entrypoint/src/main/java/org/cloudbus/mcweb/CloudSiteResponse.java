package org.cloudbus.mcweb;

import com.google.common.base.Preconditions;

/**
 * Response received from the admission controller, after an enquiry for a given
 * user.
 * 
 * @author nikolay.grozev
 *
 */
public class CloudSiteResponse {

    private final boolean eligible;
    private final double costEstimation;
    private final CloudSite site;

    /**
     * Constr.
     * 
     * @param eligible
     *            - whether the user is eligible for the cloud site.
     * @param costEstimation
     *            - an estimation of the cost for serving the user. Can be NaN
     *            the user is not eligible. Otherwise must non-negative.
     * @param site
     *            - the cloud site, whose response this is. Must not be null.
     */
    public CloudSiteResponse(final boolean eligible, final double costEstimation, final CloudSite site) {
        Preconditions.checkArgument((!Double.isNaN(costEstimation) || eligible) || costEstimation >= 0);
        Preconditions.checkNotNull(site);

        this.eligible = eligible;
        this.costEstimation = costEstimation;
        this.site = site;
    }

    /**
     * Returns whether the user is eligible for the cloud site.
     * 
     * @return whether the user is eligible for the cloud site.
     */
    public boolean isEligible() {
        return eligible;
    }

    /**
     * Returns an estimation of the cost for serving the user.
     * 
     * @return an estimation of the cost for serving the user. Can be NaN if
     *         eligible the user is not eligible.
     */
    public double getCostEstimation() {
        return costEstimation;
    }

    /**
     * Returns the cloud site, whose response this is.
     * 
     * @return the cloud site, whose response this is.
     */
    public CloudSite getCloudSite() {
        return site;
    }
}
