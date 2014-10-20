package org.cloudbus.mcweb.entrypoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import com.google.common.io.Closer;

import static org.cloudbus.mcweb.entrypoint.EntryPointConfigUtil.*;

/**
 * Represents an entry point in the system. Callers must call one of the
 * {@link configureInstance} methods before using {@link getInstance}.
 * 
 * @author nikolay.grozev
 *
 */
public final class EntryPoint implements AutoCloseable {

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
    /** The thread, submitting requests to cloud sites (admission controllers). */
    private TimerTask bacthRequestTimerTask = new TimerTask() {
        @Override
        public void run() {
            processRequests();
        }
    };

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
        if (instance == null) {
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
            final InputStream configStream, 
            final Function<String[], CloudSite> cloudSiteFactory,
            final IGeolocationService geoLocationService) {
        Preconditions.checkNotNull(cloudSitesStream);
        Preconditions.checkNotNull(configStream);
        Preconditions.checkNotNull(geoLocationService);
        Preconditions.checkNotNull(cloudSiteFactory);

        //Release previous resources.
        if (instance != null){
            try {
                instance.close();
            } catch (Exception e) {
                throw new IllegalStateException("Could not close previous EntryPoint", e);
            }
            instance = null;
        }
        
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
            final InputStream configStream, 
            final Function<String[], CloudSite> cloudSiteFactory) {
        configureInstance(cloudSitesStream, configStream, cloudSiteFactory, new GeoIP2PingERService());
    }
    
    private void processRequests() {
        synchronized (lock) {
            final CountDownLatch latch = new CountDownLatch(cloudSites.size());
            final List<UserRequest> userRequestsDeepCopy = new ArrayList<UserRequest>(userRequests);
            for (CloudSite cloudSite : cloudSites) {
                cloudSitesThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(!userRequestsDeepCopy.isEmpty()) {
                                cloudSite.enquire(userRequestsDeepCopy);
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }

            try {
                latch.await(cloudSiteResponseTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Could not get the results of all clouds", e);
            }

            userRequests.clear();
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
                bacthRequestTimer.schedule(bacthRequestTimerTask, 10, periodBetweenBatchUserDispatch);
            }
            
            // Add the request to the queue of requests to send
            userRequests.add(req);

            // Blocks until the request is served.
            try {
                if (!req.isProcessed()) {
                    lock.wait(maxRequestPeriod);
                }
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Waiting interrupted", e);
            }
        }
    }

    /**
     * Call in the end of the application.
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        synchronized (EntryPoint.class) {
            Closer closer = Closer.create();
            
            // Stop the thread, which submits the requests
            closer.register(() -> bacthRequestTimerTask.cancel());
            closer.register(() -> {
                if (bacthRequestTimer != null) {
                    bacthRequestTimer.purge();
                    bacthRequestTimer = null;
                }
            });

            // Notify all waiting threads
            closer.register(() -> {
                synchronized (lock) {
                    lock.notifyAll();
                }
            });

            // Stop all threads from the pool
            closer.register(() -> cloudSitesThreadPool.shutdown());
            
            // Stop all cloud sites
            for (CloudSite cloudSite : cloudSites) {
                closer.register(() -> {
                    try {
                        cloudSite.close();   
                    } catch (Exception e) {
                        // because closer uses Closable, not AutoClosable
                        throw new IOException(e);
                    }
                });
            }
            
            //Close them all
            closer.close();
        }
    }

    
    @SuppressWarnings("unused")
    private static void close(Iterator<AutoCloseable> closables) throws Exception {
        if (closables.hasNext()) {
            try {
                closables.next().close();
            } finally{
                close(closables);
            }
        }
    }
}
