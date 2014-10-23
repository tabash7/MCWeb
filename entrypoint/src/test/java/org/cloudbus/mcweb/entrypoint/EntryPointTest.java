package org.cloudbus.mcweb.entrypoint;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.LookUpGeoLocationService;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.cloudbus.mcweb.AggregatedUncaghtExceptionHandler;
import org.cloudbus.mcweb.entrypoint.CloudSite;
import org.cloudbus.mcweb.entrypoint.EntryPoint;
import org.cloudbus.mcweb.entrypoint.PredefinedCostCloudSite;
import org.cloudbus.mcweb.entrypoint.EPUserRequest;

import static org.cloudbus.mcweb.util.Tests.*;

public class EntryPointTest {

    private static final String CONFIG_PROPERTIES = "/config.properties";
    private static final String CLOUDSITES_PROPERTIES = "/cloudsites.properties";

    @After
    public void tearDown() throws Exception {
        EntryPoint.getInstance().close();
    }

    @Test
    public void testConfigureInstanceMultipleTimes() {
        IGeolocationService geoLocationService = new GeoIP2PingERService();
        EntryPoint.getInstance().configure(classLoad(CLOUDSITES_PROPERTIES),
                classLoad(CONFIG_PROPERTIES),
                CloudSite.FACTORY,
                geoLocationService);
        EntryPoint.getInstance().configure(classLoad(CLOUDSITES_PROPERTIES),
                classLoad(CONFIG_PROPERTIES),
                CloudSite.FACTORY,
                geoLocationService);
    }

    @Test
    public void testSingleValidUser() {
        // The user to test with
        EPUserRequest req = new EPUserRequest("127.127.127.127", "user1");
        assertFalse(req.isProcessed());

        // Latencies between user and cloud sites
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + "127.0.0.1", 20d, req.getIpAddress()
                + "127.0.0.2", 50d, req.getIpAddress() + "127.0.0.3", 15d, req.getIpAddress() + "127.0.0.4", 40d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Cloud Site factory - user is eligible for cloud site 1 and 3
        Iterable<Set<String>> eligibleUsers = Arrays.asList(new HashSet<String>(Arrays.asList(req.getUserToken())),
                new HashSet<String>(Arrays.asList()), new HashSet<String>(Arrays.asList(req.getUserToken())),
                new HashSet<String>(Arrays.asList()));
        Iterable<Double> costs = Arrays.asList(20d, 30d, 19d, 50d);
        Function<String[], CloudSite> factory = new PredefinedCostCloudSite.PredefinedCostCloudSiteFactory(
                eligibleUsers, costs, null);

        // Configure the entry point
        EntryPoint.getInstance().configure(classLoad(CLOUDSITES_PROPERTIES),
                classLoad(CONFIG_PROPERTIES),
                factory,
                geoLocationService);

        // Send the request
        EntryPoint.getInstance().request(req);

        // Assert properties are set
        assertEquals(40, req.getLatencySLA(), 0.01);
        assertTrue(req.isProcessed());
        assertEquals("AWS3", req.selectCloudSite().getName());
    }

    @Test
    public void testSingleInvalidUser() {
        // The user to test with
        EPUserRequest req = new EPUserRequest("127.127.127.127", "user1");
        assertFalse(req.isProcessed());

        // Latencies between user and cloud sites
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + "127.0.0.1", 20d, req.getIpAddress()
                + "127.0.0.2", 50d, req.getIpAddress() + "127.0.0.3", 15d, req.getIpAddress() + "127.0.0.4", 40d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Cloud Site factory - user is not eligible anywhere
        Iterable<Set<String>> eligibleUsers = Arrays.asList(new HashSet<String>(Arrays.asList()), new HashSet<String>(
                Arrays.asList()), new HashSet<String>(Arrays.asList()), new HashSet<String>(Arrays.asList()));
        Iterable<Double> costs = Arrays.asList(20d, 30d, 19d, 50d);
        Function<String[], CloudSite> factory = new PredefinedCostCloudSite.PredefinedCostCloudSiteFactory(
                eligibleUsers, costs, null);

        // Configure the entry point
        EntryPoint.getInstance().configure(classLoad(CLOUDSITES_PROPERTIES),
                classLoad(CONFIG_PROPERTIES),
                factory,
                geoLocationService);

        // Send the request
        EntryPoint.getInstance().request(req);

        // Assert properties are sep
        assertEquals(40, req.getLatencySLA(), 0.01);
        assertTrue(req.isProcessed());
        assertNull(req.selectCloudSite());
    }

    @Test
    public void testMultipleUsersNoDelays() throws Throwable {
        testMultipleUsersWithDelays(null, 0);
    }

    @Test
    public void testMultipleUsersDelays() throws Throwable {
        // Average delay
        testMultipleUsersWithDelays(Arrays.asList(100l, 100l, 1000l, 200l), 500l);

        // Small user arrival delay, large cloud site reponse delay
        testMultipleUsersWithDelays(Arrays.asList(200l, 1000l, 2100l, 2100l), 50l);

        // Small cloud site response delay, large user arrival delay
        testMultipleUsersWithDelays(Arrays.asList(50l, 10l, 60l, 70l), 5000l);

        // Large cloud site response delay, large user arrival delay
        testMultipleUsersWithDelays(Arrays.asList(200l, 1000l, 2100l, 2100l), 5000l);
    }

    @Test
    public void testUnresponsiveCloudWithoutAlternative() throws Throwable {
        // Unresponsive cloud site
        testMultipleUsersWithDelays(Arrays.asList(10_000_000l, 10l, 60l, 70l), 50l);
    }

    @Test
    public void testUnresponsiveCloudWithAlternative() throws Throwable {
        // Unresponsive cloud site
        testMultipleUsersWithDelays(Arrays.asList(10l, 10_000_000l, 60l, 70l), 50l);
    }

    @Test
    public void testTwoUnresponsiveCloudsWithAlternative() throws Throwable {
        // Unresponsive cloud site
        testMultipleUsersWithDelays(Arrays.asList(10l, 10_000_000l, 60l, 10_000_000l), 50l);
    }

    /**
     * Creates 100 users whose requests' arrival times are uniformly distributed
     * in the interval [0, requestDelays], and submits them the EntryPoint. The
     * Entry point is configured with 4 cloud sites, the serving delays of which
     * are specified in cloudsiteDelays.
     * 
     * <br>
     * 
     * The method tests a complicated scenario, where each user should be
     * assigned in an eligible cloud, if one is available in accordance with the
     * optimisation algorithm.
     * 
     * @param cloudsiteDelays
     * @param requestDelays
     * @throws Throwable 
     */
    private void testMultipleUsersWithDelays(final List<Long> cloudsiteDelays, final long requestDelays)
            throws Throwable {
        final Random random = new Random(requestDelays);

        // The users to test with
        List<EPUserRequest> reqs = new ArrayList<EPUserRequest>();
        for (int i = 0; i < 100; i++) {
            EPUserRequest req = new EPUserRequest("127.127.127." + i, "user" + i);
            assertFalse(req.isProcessed());
            reqs.add(req);
        }

        // Latencies between users and cloud sites
        Map<String, Double> latencyCache = new HashMap<>();
        for (int i = 0; i < reqs.size(); i++) {
            EPUserRequest req = reqs.get(i);
            latencyCache.put(req.getIpAddress() + "127.0.0.1", (double) ((i + 1) * 10 % 50));
            latencyCache.put(req.getIpAddress() + "127.0.0.2", (double) ((i + 1) * 20 % 50));
            latencyCache.put(req.getIpAddress() + "127.0.0.3", (double) ((i + 1) * 30 % 50));
            latencyCache.put(req.getIpAddress() + "127.0.0.4", (double) ((i + 1) * 40 % 50));
        }

        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Cloud Site factory - define eligibilities and costs. Make a user
        // uneleigible everywhere if
        // i%4 = 0. Eligible in cloud 1 if i%4 = 1. Eligible in clouds 2 and 3
        // if i%4 = 2.
        // Eligible in clouds 2 and 4 if i%4 = 3.
        List<Set<String>> eligibleUsers = Arrays.asList(new HashSet<String>(), new HashSet<String>(),
                new HashSet<String>(), new HashSet<String>());
        List<Double> costs = Arrays.asList(10d, 20d, 30d, 40d);

        for (int i = 0; i < reqs.size(); i++) {
            EPUserRequest req = reqs.get(i);
            if (i % 4 == 0) {
                continue;
            } else if (i % 4 == 1) {
                eligibleUsers.get(0).add(req.getUserToken());
            } else if (i % 4 == 2) {
                eligibleUsers.get(1).add(req.getUserToken());
                eligibleUsers.get(2).add(req.getUserToken());
            } else if (i % 4 == 3) {
                eligibleUsers.get(1).add(req.getUserToken());
                eligibleUsers.get(3).add(req.getUserToken());
            }
        }

        Function<String[], CloudSite> factory = new PredefinedCostCloudSite.PredefinedCostCloudSiteFactory(
                eligibleUsers, costs, cloudsiteDelays);

        // Configure the entry point
        EntryPoint.getInstance().configure(classLoad(CLOUDSITES_PROPERTIES),
                classLoad(CONFIG_PROPERTIES),
                factory,
                geoLocationService);

        // Send the requests in multiple threads
        AggregatedUncaghtExceptionHandler errHandler = new AggregatedUncaghtExceptionHandler();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < reqs.size(); i++) {
            final EPUserRequest req = reqs.get(i);
            Thread t = new Thread(() -> {
                sleep(random.nextDouble() * requestDelays);
                EntryPoint.getInstance().request(req);
            });
            t.setName(req.getUserToken());
            t.setUncaughtExceptionHandler(errHandler);
            threads.add(t);
            t.start();
        }

        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }
        
        // If any thread has failed - raise the first exception
       errHandler.throwFirst();

        // Assert properties are set properly
        for (int i = 0; i < reqs.size(); i++) {
            EPUserRequest req = reqs.get(i);
            assertTrue(req.isProcessed());
            assertEquals(40, req.getLatencySLA(), 0.01);

            @SuppressWarnings("unused")
            double latency1 = (double) ((i + 1) * 10 % 50);
            double latency2 = (double) ((i + 1) * 20 % 50);
            double latency3 = (double) ((i + 1) * 30 % 50);
            double latency4 = (double) ((i + 1) * 40 % 50);

            boolean unresposnive1 = cloudsiteDelays != null && cloudsiteDelays.get(0) > 6000;
            boolean unresposnive2 = cloudsiteDelays != null && cloudsiteDelays.get(1) > 6000;
            boolean unresposnive3 = cloudsiteDelays != null && cloudsiteDelays.get(2) > 6000;
            boolean unresposnive4 = cloudsiteDelays != null && cloudsiteDelays.get(3) > 6000;

            if (i % 4 == 0) {
                assertNull(req.selectCloudSite());
            } else if (i % 4 == 1) {
                if (unresposnive1) {
                    assertNull(req.selectCloudSite());
                } else {
                    assertEquals("AWS1", req.selectCloudSite().getName());
                }
            } else if (i % 4 == 2) {
                if (unresposnive2 && unresposnive3) {
                    assertNull(req.selectCloudSite());
                } else if (unresposnive3) {
                    assertEquals("AWS2", req.selectCloudSite().getName());
                } else if (unresposnive2) {
                    assertEquals("AWS3", req.selectCloudSite().getName());
                } else if (latency2 >= 40 || latency3 > 40) {
                    assertEquals(latency2 < latency3 ? "AWS2" : "AWS3", req.selectCloudSite().getName());
                } else {
                    assertEquals(costs.get(1) < costs.get(2) ? "AWS2" : "AWS3", req.selectCloudSite().getName());
                }
            } else if (i % 4 == 3) {
                if (unresposnive2 && unresposnive4) {
                    assertNull(req.selectCloudSite());
                } else if (unresposnive4) {
                    assertEquals("AWS2", req.selectCloudSite().getName());
                } else if (unresposnive2) {
                    assertEquals("AWS4", req.selectCloudSite().getName());
                } else if (latency2 >= 40 || latency4 > 40) {
                    assertEquals(unresposnive4 || latency2 < latency4 ? "AWS2" : "AWS4", req.selectCloudSite()
                            .getName());
                } else {
                    assertEquals(unresposnive4 || costs.get(1) < costs.get(3) ? "AWS2" : "AWS4", req.selectCloudSite()
                            .getName());
                }
            }
        }
    }

    public static InputStream classLoad(final String resource) {
        Preconditions.checkNotNull(resource);
        return EntryPointTest.class.getResourceAsStream(resource);
    }
}
