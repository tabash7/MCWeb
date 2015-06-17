package org.cloudbus.mcweb.entrypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;
import org.cloudbus.mcweb.UserRequest;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents the information related to a user, attempting to login into the
 * system. An instance of this class acts as a "black board", where multiple
 * entities "write" information about the user request.
 * 
 * <br>
 * <br>
 * 
 * More specifically, instances of this class, typically get their state
 * initialised in stages:
 * <ul>
 * <li>The service caller specifies the IP address and user token.</li>
 * <li>Then the {@link EntryPoint} specifies the preconfigured latency SLA and
 * geolocation services.</li>
 * <li>Finally, the {@link CloudSite}-s specify the details of the pottential
 * connection (e.g. cost, eleibility).</li>
 * </ul>
 * 
 * <br>
 * 
 * As some of these run in different threads, all methods of this class are
 * synchronised.
 * 
 * @author nikolay.grozev
 *
 */
public class EPUserRequest extends UserRequest {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(EPUserRequest.class.getCanonicalName());

    final List<EPAdmissionControllerResponse> viableCloudSiteResponses;
    private boolean processed = false;
    private double latencySLA = -1;
    private IGeolocationService geoLocationService;

    private static final Comparator<EPAdmissionControllerResponse> COST_CMP = new Comparator<EPAdmissionControllerResponse>() {
        @Override
        public int compare(final EPAdmissionControllerResponse site1, final EPAdmissionControllerResponse site2) {
            return Double.compare(site1.getCostEstimation(), site2.getCostEstimation());
        }
    };

    /**
     * Constr.
     * 
     * @param ipAddress
     *            - the IP address of the user. Must not be null. Should be
     *            either IPv4 or IPv6. IPv4 addresses should be in the standard
     *            dotted form (e.g. 1.2.3.4). IPv6 addresses should be in the
     *            canonical form described in RFC 5952 - e.g. 2001:db8::1:0:0:1.
     * @param userToken
     */
    public EPUserRequest(final String ipAddress, final String userToken) {
        super(ipAddress, userToken);
        this.viableCloudSiteResponses = new ArrayList<>();
    }

    /**
     * Registers this response to this request.
     * 
     * @param response
     *            - the new response. Must no be null.
     */
    public synchronized void addResponseFromCloudSite(final EPAdmissionControllerResponse response) {
        Preconditions.checkNotNull(response);
        Preconditions.checkArgument(response.getUserToken().equals(getUserToken()));
        LOG.log(Level.INFO, "User {0}, Receiving response from {1}, Eligibility={2}, Cost={3} ", 
                new Object[] {toString(),
                    response.getCloudSite().getName(),
                    response.isEligible(),
                    response.getCostEstimation() });

        if (response.isEligible()) {
            // Keep it sorted
            int pos = Collections.binarySearch(viableCloudSiteResponses, response, COST_CMP);
            if (pos < 0) {
                pos = -pos - 1;
            }
            viableCloudSiteResponses.add(pos, response);
        }
        setProcessed(true);
    }

    /**
     * Sets this request as processed.
     * 
     * @param processed
     *            - the processed flag.
     */
    public synchronized void setProcessed(final boolean processed) {
        this.processed = processed;
    }

    /**
     * Returns if this request is processed.
     * 
     * @return if this request is processed.
     */
    public synchronized boolean isProcessed() {
        return processed;
    }

    /**
     * Returns the latency SLA.
     * 
     * @return the latency SLA.
     */
    public synchronized double getLatencySLA() {
        return latencySLA;
    }

    /**
     * Sets the latency SLA. This must be set, before calling
     * {@link selectCloudSite}
     * 
     * @param latencySLA
     *            - the new latency. Must be positive.
     */
    public synchronized void setLatencySLA(final double latencySLA) {
        Preconditions.checkArgument(latencySLA > 0);
        this.latencySLA = latencySLA;
    }

    /**
     * Sets the geo-location service to use for the selection of a cloud site.
     * 
     * @param geoLocationService
     *            - the geolocation service. Must not be null.
     */
    public synchronized void setGeoLocationService(final IGeolocationService geoLocationService) {
        Preconditions.checkNotNull(geoLocationService);
        this.geoLocationService = geoLocationService;
    }

    /**
     * Returns the best cloud site, or null if the user should be refused
     * access.
     * 
     * @return the best cloud site, or null if the user should be refused
     *         access.
     */
    public synchronized CloudSite selectCloudSite() {
        Preconditions.checkArgument(latencySLA > 0);
        Preconditions.checkNotNull(geoLocationService);

        CloudSite selectedCloud = null;
        double selectedLatency = Double.MAX_VALUE;

        for (EPAdmissionControllerResponse cloudSiteResponse : viableCloudSiteResponses) {
            double latency = geoLocationService
                    .latency(getIpAddress(), cloudSiteResponse.getCloudSite().getIPAddress());
            if (Double.isNaN(latency)) {
                LOG.log(Level.WARNING, 
                        "Could not find the latency between {0} and {1} Considering the latency to be the SLA: {2}", 
                        new Object[] { getIpAddress(),
                                       cloudSiteResponse.getCloudSite().getIPAddress(),
                                       latencySLA });
                latency = latencySLA;
            } /*else {
            	LOG.log(Level.WARNING, 
                        "-->> Latency between {0} and {1} is: {2}", 
                        new Object[] { getIpAddress(),
                                       cloudSiteResponse.getCloudSite().getIPAddress(),
                                       latency });
            }*/
            
            if (latency < latencySLA) {
                selectedCloud = cloudSiteResponse.getCloudSite();
                selectedLatency = latency;
                break;
            } else if (selectedLatency > latency) {
                selectedCloud = cloudSiteResponse.getCloudSite();
                selectedLatency = latency;
            }
        }

        LOG.log(Level.WARNING, "User {0}, Selected cloud site {1}, Latency: {2} \n\n",
                new Object[] {toString(), java.util.Objects.toString(selectedCloud).toString(), selectedLatency });
        
        return selectedCloud;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("User", this.getUserToken())
                .add("IP", this.getIpAddress())
                .add("LatencySLA", this.latencySLA)
                .add("Processed", this.processed)
                .toString();
    }
}
