package org.cloudbus.mcweb;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents the information related to a user, attempting to login into the
 * system.
 * 
 * @author nikolay.grozev
 *
 */
public class UserRequest {

    private final String ipAddress;
    private final String userToken;

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
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("User", this.getUserToken())
                .add("IP", this.getIpAddress())
                .toString();
    }
}