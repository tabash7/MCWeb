package org.cloudbus.mcweb.entrypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Preconditions;

/**
 * A CloudSite, whose cost per user and set of eligible users are predefined.
 * Also has the option to respond to enquiries with a delay.
 * 
 * @author nikolay.grozev
 *
 */
public class PredefinedCostCloudSite extends CloudSite {

    private final Set<String> eligibleUsers;
    private final double cost;
    private final long enquiryDelay;

    /**
     * Constr.
     * 
     * @param name
     *            - see superclass.
     * @param admissionControllerAddresse
     *            - see superclass.
     * @param loadBalancerAddresse
     *            - see superclass.
     * @param eligibleUsers
     *            - which users (identified with their user token) are eligible.
     *            if null all users are considered eligible.
     * @param cost
     *            - the cost for serving in this data centre. Must not be
     *            negative.
     * @param enquiryDelay
     *            - how many milliseconds to wait before responding to an
     *            enquiry. Must not be negative.
     */
    public PredefinedCostCloudSite(final String name, final String admissionControllerAddress,
            final String loadBalancerAddress, final Set<String> eligibleUsers, final double cost,
            final long enquiryDelay) {
        super(name, admissionControllerAddress, loadBalancerAddress);
        Preconditions.checkArgument(cost >= 0);
        Preconditions.checkArgument(enquiryDelay >= 0);
        
        this.eligibleUsers = eligibleUsers != null ? Collections.unmodifiableSet(eligibleUsers) : null;
        this.cost = cost;
        this.enquiryDelay = enquiryDelay;
    }

    @Override
    public void enquire(List<EPUserRequest> requests) {
        Preconditions.checkNotNull(requests);
        Preconditions.checkArgument(!requests.isEmpty());
        if(enquiryDelay > 0) {
            try {
                Thread.sleep(enquiryDelay);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        for (EPUserRequest userRequest : new ArrayList<>(requests)) {
            boolean eligible = eligibleUsers == null ? true : eligibleUsers.contains(userRequest.getUserToken());
            userRequest.addResponseFromCloudSite(new EPAdmissionControllerResponse(userRequest.getUserToken(), eligible, cost, this));
        }
    }
    
    /**
     * Creates {@link PredefinedCostCloudSite} .
     */
    public static class PredefinedCostCloudSiteFactory implements Function<String[], CloudSite> {

        private final Iterator<Set<String>> eligibleUsers;
        private final Iterator<Double> costs;
        private final Iterator<Long> enquiryDelays;
        
        public PredefinedCostCloudSiteFactory(Iterable<Set<String>> eligibleUsers,
                Iterable<Double> costs,
                Iterable<Long> enquiryDelays) {
            super();
            this.eligibleUsers = eligibleUsers.iterator();
            this.costs = costs.iterator();
            this.enquiryDelays = enquiryDelays == null ? null : enquiryDelays.iterator();
        }

        @Override
        public CloudSite apply(String[] s) {
            return new PredefinedCostCloudSite(s[0], s[1], s[2], 
                    this.eligibleUsers.next(), 
                    this.costs.next(),
                    this.enquiryDelays == null ? 0 : enquiryDelays.next());
        }
    }
}
