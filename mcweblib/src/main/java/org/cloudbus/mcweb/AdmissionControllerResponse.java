package org.cloudbus.mcweb;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Response received from the admission controller, after an enquiry for a given
 * user.
 * 
 * @author nikolay.grozev
 *
 */
public class AdmissionControllerResponse {

    private final String userToken;
    private final boolean eligible;
    private final double costEstimation;

    /**
     * Constr.
     * 
     * @param userToken
     *            - the user token. Must not be null;
     * @param eligible
     *            - whether the user is eligible for the cloud site.
     * @param costEstimation
     *            - an estimation of the cost for serving the user. Can be NaN
     *            the user is not eligible. Otherwise must non-negative.
     */
    public AdmissionControllerResponse(final String userToken, final boolean eligible, final double costEstimation) {
        Preconditions.checkNotNull(userToken);
        Preconditions.checkArgument((Double.isNaN(costEstimation) && !eligible) || costEstimation >= 0);

        this.userToken = userToken;
        this.eligible = eligible;
        this.costEstimation = costEstimation;
    }
    
    /**
     * Returns the user token.
     * @return the user token.
     */
    public String getUserToken() {
        return userToken;
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
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("User", this.getUserToken())
                .add("Cost est", this.getCostEstimation())
                .add("Eligible", this.isEligible())
                .toString();
    }
}