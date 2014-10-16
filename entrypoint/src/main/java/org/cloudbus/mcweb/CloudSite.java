package org.cloudbus.mcweb;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

/**
 * Represents a cloud site location - with an admission controller and a load
 * balancer. Manages the connection with the cloud site.
 * 
 * @author nikolay.grozev
 *
 */
public class CloudSite {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(CloudSite.class.getCanonicalName());

    /** A human readable name of the cloud site. */
    private final String name;
    /** The URL address of the admission controller in the cloud site. */
    private final String admissionControllerAddress;
    /** The URL address of the load balancer in the cloud site. */
    private final String loadBalancerAddress;
    /** The IP address of the load balancer, extracted after DNS resolution. */
    private final String ipAddress;

    /**
     * Constr.
     * 
     * @param name
     *            - human readable name of the cloud site. Must not be null.
     * @param admissionControllerAddress
     *            - address of the admission controller. Must not be null.
     * @param loadBalancerAddress
     *            - address of the load balancer. Must not be null.
     */
    public CloudSite(final String name, final String admissionControllerAddress, final String loadBalancerAddress) {
        super();
        this.name = name;
        this.admissionControllerAddress = admissionControllerAddress;
        this.loadBalancerAddress = loadBalancerAddress;

        // Force DNS resolution to retrieve the actual IP address.
        String bareUrl = loadBalancerAddress.replaceAll("^\\w+://", "").replaceAll("/.*$", "");
        try {
            InetAddress inetAddress = InetAddress.getByName(bareUrl);
            this.ipAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            String msg = "Can not resolve the DNS \"" + bareUrl + "\" address of the load balancer";
            LOG.log(Level.SEVERE, msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    /**
     * Returns the human readable name of the cloud site.
     * 
     * @return the human readable name of the cloud site.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the admission controller address.
     * 
     * @return the admission controller address.
     */
    public String getAdmissionControllerAddress() {
        return admissionControllerAddress;
    }

    /**
     * Returns the load balancer's address.
     * 
     * @return the load balancer's address.
     */
    public String getLoadBalancerAddress() {
        return loadBalancerAddress;
    }

    /**
     * Enquires the cloud site about the user.
     * 
     * @param requests
     *            - a unique identifier of the user. Must not be null or empty.
     */
    public void enquire(final List<UserRequest> requests) {
        Preconditions.checkNotNull(requests);
        Preconditions.checkArgument(!requests.isEmpty());
        for (UserRequest userRequest : requests) {
            userRequest.addResponseFromCloudSite(new CloudSiteResponse(true, 0, this));
        }
    }

    /**
     * Returns the IP address (not a DNS name) in the proper format.
     * 
     * @return the IP address (not a DNS name) in the proper format.
     */
    public String getIPAddress() {
        return ipAddress;
    }

}
