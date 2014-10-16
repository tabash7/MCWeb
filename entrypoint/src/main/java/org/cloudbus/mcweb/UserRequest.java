package org.cloudbus.mcweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;

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
public class UserRequest {

    /** Logger. */
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(UserRequest.class.getCanonicalName());
    
    private final String ipAddress;
    private final String userToken;
    private final List<CloudSiteResponse> viableCloudSiteResponses;
    private boolean processed = false;
    private double latencySLA = -1;
    private IGeolocationService geoLocationService;

    private static final Comparator<CloudSiteResponse> COST_CMP = new Comparator<CloudSiteResponse>() {
        @Override
        public int compare(final CloudSiteResponse site1, final CloudSiteResponse site2) {
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
    public UserRequest(final String ipAddress, final String userToken) {
        Preconditions.checkNotNull(ipAddress);
        Preconditions.checkNotNull(userToken);

        this.ipAddress = ipAddress;
        this.userToken = userToken;
        this.viableCloudSiteResponses = new ArrayList<>();
    }

    /**
     * Returns the IP address of the user.
     * 
     * @return the IP address of the user.
     */
    public synchronized String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the user token, identifying the user.
     * 
     * @return the user token, identifying the user.
     */
    public synchronized String getUserToken() {
        return userToken;
    }

    /**
     * Registers this response to this request.
     * 
     * @param response
     *            - the new response. Must no be null.
     */
    public synchronized void addResponseFromCloudSite(final CloudSiteResponse response) {
        Preconditions.checkNotNull(response);

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

        for (CloudSiteResponse cloudSiteResponse : viableCloudSiteResponses) {
            double latency = geoLocationService.latency(getIpAddress(), cloudSiteResponse.getCloudSite().getIPAddress());
            if (latency < latencySLA) {
                selectedCloud = cloudSiteResponse.getCloudSite();
                break;
            } else if (selectedLatency > latency) {
                selectedCloud = cloudSiteResponse.getCloudSite();
                selectedLatency = latency;
            }
        }
        return selectedCloud;
    }

}
