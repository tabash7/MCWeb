package org.cloudbus.mcweb;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;

import com.google.common.base.Preconditions;

import static org.cloudbus.mcweb.ConfigUtil.*;

/**
 * Represents an entry point in the system.
 * 
 * @author nikolay.grozev
 *
 */
public final class EntryPoint {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(EntryPoint.class.getCanonicalName());

    /** List of all cloud sites in the multi-cloud env. */
    private List<CloudSite> cloudSites = new ArrayList<>();
    /** List of the incoming users, which have not been redirected yet. */
    private List<UserRequest> userRequests = new ArrayList<>();
    /** ThreadPool for connecting to admission controllers asynch. */
    private final ExecutorService cloudSitesThreadPool = Executors.newCachedThreadPool();

    /** A geo-location services to determine the latencies between hosts. */
    private IGeolocationService geoLocationService;

    /** A lock obj to synch the access to the not yet served user requests. */
    private final Object lock = new Object();
    /** Periodically submits requests to cloud sites (admission controllers). */
    private Timer bacthRequestTimer = null;

    /*
     * Members below are read from configuration
     */
    /** The target latency SLA. */
    private double latencySLA = -1;
    /** Period for the Timer. */
    private long periodBetweenBatchUserDispatch = -1;
    /** How long to wait for a cloud sites (admission controller) to respond. */
    private long cloudSiteResponseTimeout = -1;
    /** How long can a request wait. */
    private long maxRequestPeriod = -1;

    /** Singleton instance. */
    private static EntryPoint instance;

    /** Suppress instantiation. */
    private EntryPoint() {
    }

    /**
     * Returns the only instance of the singleton.
     * 
     * @return the only instance of the singleton.
     */
    public static synchronized EntryPoint getInstance() {
        if (instance != null) {
            instance = new EntryPoint();
        }
        return instance;
    }

    /**
     * Configures the instance with the specified streams.
     * 
     * @param cloudSitesStream
     *            - the stream to read the cloud sites from. Must be in the
     *            valid format. Will be closed as a result.
     * @param configStream
     *            - the stream to read the configuration from. Must be in the
     *            valid format. Will be closed as a result.
     * @param cloudSiteFactory
     *            - a factory function, which creates a CloudSite, based on an
     *            array of [name, admissionControllerAddress,
     *            loadBalancerAddress]. Must not be null.
     * @param geoLocationService
     *            - the geolocation service to use. Must not be null.
     */
    public static synchronized void configureInstance(final InputStream cloudSitesStream,
            final InputStream configStream, final Function<String[], CloudSite> cloudSiteFactory,
            final IGeolocationService geoLocationService) {
        Preconditions.checkNotNull(geoLocationService);
        Preconditions.checkNotNull(cloudSiteFactory);

        getInstance().cloudSites = Collections.unmodifiableList(parseCloudSites(cloudSitesStream, cloudSiteFactory));

        Properties props = parseConfig(configStream);
        getInstance().latencySLA = Double.parseDouble(props.getProperty(LATENCY_SLA_PROP));
        getInstance().periodBetweenBatchUserDispatch = Long.parseLong(props
                .getProperty(PERIOD_BETWEEN_BATCH_USER_DISPATCH_PROP));
        getInstance().cloudSiteResponseTimeout = Long.parseLong(props.getProperty(CLOUD_SITE_RESPONSE_TIMEOUT_PROP));
        getInstance().maxRequestPeriod = Long.parseLong(props.getProperty(MAX_REQUEST_PERIOD_PROP));

        getInstance().geoLocationService = geoLocationService;
    }

    /**
     * Configures the instance with the specified streams.
     * 
     * @param cloudSitesStream
     *            - the stream to read the cloud sites from. Must be in the
     *            valid format. Will be closed as a result.
     * @param configStream
     *            - the stream to read the configuration from. Must be in the
     *            valid format. Will be closed as a result.
     * @param cloudSiteFactory
     *            - a factory function, which creates a CloudSite, based on an
     *            array of [name, admissionControllerAddress,
     *            loadBalancerAddress]. Must not be null.
     */
    public static synchronized void configureInstance(final InputStream cloudSitesStream,
            final InputStream configStream, final Function<String[], CloudSite> cloudSiteFactory) {
        configureInstance(cloudSitesStream, configStream, cloudSiteFactory, new GeoIP2PingERService());
    }
    
    private void processRequests() {
        final CountDownLatch latch = new CountDownLatch(cloudSites.size());
        synchronized (lock) {
            for (CloudSite cloudSite : cloudSites) {
                cloudSitesThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cloudSite.enquire(userRequests);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            userRequests.clear();
        }

        try {
            latch.await(cloudSiteResponseTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Could not get the results of all clouds", e);
        }

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void request(final UserRequest req) {
        Preconditions.checkNotNull(req);

        synchronized (lock) {
            // Set the latency SLA of the request and geolocation service
            req.setLatencySLA(latencySLA);
            req.setGeoLocationService(geoLocationService);

            // Start the timer, which submits the requests to the admission
            // controller, if it has not been started
            if (bacthRequestTimer == null) {
                bacthRequestTimer = new Timer("Entry Point Batch Request Timer", true);
                bacthRequestTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        processRequests();
                    }
                }, 10, periodBetweenBatchUserDispatch);
            }

            // Add the request to the queue of requests to send
            userRequests.add(req);

            // Blocks until the request is served.
            try {
                while (!req.isProcessed()) {
                    lock.wait(maxRequestPeriod);
                }
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Waiting interrupted", e);
            }
        }
    }

}
