package org.cloudbus.mcweb;

import java.util.function.Function;

/**
 * Utility class for parsing the configuration files.
 * 
 * @author nikolay.grozev
 *
 */
public final class CloudSiteUtil {

    /** Suppress instantiation. */
    private CloudSiteUtil() {
    }

    /**
     * Creates default cloud sites, which accept every user with 0 cost. The
     * created cloud sites do not communicate with the actual clouds.
     */
    public static final Function<String[], CloudSite> DEFAULT_FACTORY = s -> new CloudSite(s[0], s[1], s[2]);

}
