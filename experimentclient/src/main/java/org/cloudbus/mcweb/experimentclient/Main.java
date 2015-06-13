package org.cloudbus.mcweb.experimentclient;


import static org.cloudbus.mcweb.util.Configs.EP_PATH;
import static org.cloudbus.mcweb.util.Configs.SERVICE_PATH;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.cloudbus.mcweb.EntryPointResponse;
import org.cloudbus.mcweb.User;
import org.cloudbus.mcweb.admissioncontroller.IUserResolver;
import org.cloudbus.mcweb.admissioncontroller.TestUserResolver;
import org.cloudbus.mcweb.util.Closeables;
import org.cloudbus.mcweb.util.Jsons;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

/**
 * Starts a local admission controller. 
 * 
 * @author nikolay.grozev
 */
public class Main {

    static {
        Logger log = LogManager.getLogManager().getLogger("");
        for (Handler h : log.getHandlers()) {
            h.setLevel(Level.WARNING);
        }
    }

    private static Logger LOG = Logger.getLogger(Main.class.getCanonicalName());
    
    private static Client client;
    private static List<String[]> entryPointAddresses;
    private static long clientStartTime = System.currentTimeMillis();
    private static String clientLocation;
    private static final ExecutorService cache =  Executors.newCachedThreadPool();
    private static Random random = new Random();
    private static BufferedMultiThreadedFileWriter writer;
    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final IUserResolver USER_RESOLVER = new TestUserResolver();
    private static ConcurrentHashMap<String, WebTarget> entryPointTargets = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, WebTarget> pingTargets = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Double> latencies = new ConcurrentHashMap<>();
    private static final int[] LOG_LENS = new int[] {
            8,
            6,
            11,
            11,
            11,
            6,
            8,
            25,
            13,
            12,
            8,
            8,
            13,
            6
    };
    
    
    /**
     * Starts a test client. 
     * 
     * @param args - in the aforementioned format
     * @throws Exception - if something goes wrong
     */
    public static void main(String[] args) throws Exception {
        clientLocation = args.length > 0 ? args[0] : "Local";
        InputStream entryPointsFile = args.length > 1 ? new FileInputStream(args[1]) : Main.class.getResourceAsStream("/entrypoints.csv");
        InputStream lbFile = args.length > 2 ? new FileInputStream(args[1]) : Main.class.getResourceAsStream("/loadbalancers.txt");
        
        // Open the output file for the client
        writer = new BufferedMultiThreadedFileWriter("ClientLog_" + clientLocation + ".csv");
        writer.writeCsv(LOG_LENS,
                "Time",
                "UNum",
                "Client",
                "EPLoc",
                "SelCloud",
                "DispT",
                "Latency",
                "UserId",
                "Citizenships",
                "UserTags",
                "Provider",
                "CloudLoc",
                "DCTags",
                "Cost");
        
        // Create an HTTP client
        ClientConfig configuration = new ClientConfig();
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, 120000);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, 120000);
        client = ClientBuilder.newClient();
        Closeables.addSilentShutdownHook(client::close);
        
        // Read the address of all entry points
        entryPointAddresses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entryPointsFile))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(!line.isEmpty()) {
                    entryPointAddresses.add(line.split("\\s*;\\s*"));
                    LOG.log(Level.WARNING,"Entry Point Line: {0}", line);
                }
            }
            entryPointAddresses = Collections.unmodifiableList(entryPointAddresses);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load entry points!", e);
            throw new IllegalStateException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(lbFile))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(!line.isEmpty()) {
                	int count = 10;
                	long sum = 0;
                	for(int i = 0 ; i < count; i++) {
                		Thread.sleep(1000);
                		sum += performPing(line);
                	}
                	latencies.put(line, sum / (double)count);
                    LOG.log(Level.WARNING,"Load Balancer Line: {0}", line);
                    LOG.log(Level.WARNING,"Load Balancer Latency: {0}", latencies.get(line));
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load load balancers!", e);
            throw new IllegalStateException(e);
        }
        
        clientStartTime = System.currentTimeMillis();
        int secsBetweenRuns = 5;
        long day = 24l * 60l * 60l;
        for (long sec = 0; sec <= day; sec += secsBetweenRuns){
            startRuns(100, secsBetweenRuns);
            sleep(secsBetweenRuns);
        }
    }

    private static void startRuns(int n, int secsBetweenRuns) {
        for (int i = 0; i < n; i++) {
            cache.execute(() -> {
                sleep(secsBetweenRuns * random.nextDouble());
                runClient();
            });
        }
    }

    private static void runClient() {
        // current time when the client started
        long clientStartTime = System.currentTimeMillis();
        
        // Pick a random entry point
        int entryPointId = random.nextInt(entryPointAddresses.size());
        String[] ep = entryPointAddresses.get(entryPointId);
        String entryPointCode = ep[0];
        String entryPointAddress = ep[1];
        
        // User number and id
        int userCount = COUNT.incrementAndGet();
        String userToken = (clientLocation + userCount + "-" + Math.abs(random.nextInt(1000)));
        User u = USER_RESOLVER.resolve(userToken);
        
        // Point the client to the entry point
        WebTarget webTarget = entryPointTarget(entryPointAddress, userToken);
        EntryPointResponse response = new EntryPointResponse(null, null);
        try {
            String responseJson = webTarget.request(MediaType.APPLICATION_JSON).get(String.class);
            response = Jsons.fromJson(responseJson, EntryPointResponse.class);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, String.format("Could not load responses from entry point %s: %s", ep[0], ep[1]));
            LOG.log(Level.SEVERE, "Attempted to connect with: " + webTarget.getUri().toString());
        }
        
        // current time after the response
        long clientEndTime = System.currentTimeMillis();        
        
        Double latency = null;
        if (response.getRedirectAddress() != null) {
            latency = latencies.get(response.getRedirectAddress());
        }
        
        writer.writeCsv(LOG_LENS, clientStartTime - Main.clientStartTime,
                userCount,
                Main.clientLocation,
                entryPointCode,
                response.getSelectedCloudSiteCode(),
                clientEndTime - clientStartTime,
                latency == null ? "null" : String.format("%.2f", latency),
                userToken,
                Arrays.toString(u.getCitizenships().toArray()),
                Arrays.toString(u.getTags().toArray()),
                response.getDefinition() == null ? "null" : response.getDefinition().getProviderCode(),
                response.getDefinition() == null ? "null" : response.getDefinition().getLocationCode(),
                response.getDefinition() == null ? "null" : Arrays.toString(response.getDefinition().getTags().toArray()),
                response.getDefinition() == null ? "null" : response.getDefinition().getCost()
        );
        //writer.flush();
    }

	private static Long performPing(final String pingAddress) {
		long latency;
		// Ping ...
		WebTarget pingTarget = pingTarget(pingAddress);
		long beforePing = System.currentTimeMillis();
		@SuppressWarnings("unused")
		String pingResponse = pingTarget.request(MediaType.APPLICATION_JSON).get(String.class);
		long afterPing = System.currentTimeMillis();
		latency = (afterPing - beforePing) / 2;
		return latency;
	}

	private static WebTarget entryPointTarget(final String entryPointAddress, final String userToken) {
		if(!entryPointTargets.containsKey(entryPointAddress)) {
			entryPointTargets.putIfAbsent(entryPointAddress, client.target(entryPointAddress).path(EP_PATH).path(SERVICE_PATH));
		}
		return entryPointTargets.get(entryPointAddress).path(userToken);
	}

	private static WebTarget pingTarget(final String pingAddress) {
		if(!pingTargets.containsKey(pingAddress)) {
			pingTargets.putIfAbsent(pingAddress, client.target(pingAddress));
		}
		return pingTargets.get(pingAddress);
	}
	
	
    private static void sleep(double secs) {
        try {
            Thread.sleep(Math.round(secs * 1000));
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
