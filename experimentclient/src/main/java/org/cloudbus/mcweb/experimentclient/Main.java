package org.cloudbus.mcweb.experimentclient;

import static org.cloudbus.mcweb.util.Configs.EP_PATH;
import static org.cloudbus.mcweb.util.Configs.EP_FULL_SERVICE_PATH;
import static org.cloudbus.mcweb.util.Configs.EP_SERVICE_PATH;
import static org.cloudbus.mcweb.util.Configs.USER_TOKENS_PARAM;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

/**
 * Starts a local admission controller. 
 * 
 * @author nikolay.grozev
 */
public class Main {
    
    private static Logger LOG = Logger.getLogger(Main.class.getCanonicalName());
    
    private static Client client;
    private static List<String> entryPointAddresses;
    private static final long clientStartTime = System.currentTimeMillis();
    private static final ExecutorService cache =  Executors.newCachedThreadPool();
    private static Random random = new Random();
    private static final String OUT_FILE = "ClientLog.txt";
    
    /**
     * Starts a test client. 
     * 
     * @param args - in the aforementioned format
     * @throws Exception - if something goes wrong
     */
    public static void main(String[] args) throws Exception {
        String clientCode = args[0];
        String entryPointsFile = args[1];
        
        // Create a HTTP client
        ClientConfig configuration = new ClientConfig();
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, 20000);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, 20000);
        client = ClientBuilder.newClient();
        
        // Read the address of all entry points
        entryPointAddresses = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(entryPointsFile))) {
            String entryPointAddress = null;
            while ((entryPointAddress = reader.readLine()) != null) {
                entryPointAddress = entryPointAddress.trim();
                if(!entryPointAddress.isEmpty()) {
                    entryPointAddresses.add(entryPointAddress);
                }
            }
            entryPointAddresses = Collections.unmodifiableList(entryPointAddresses);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load entry points!", e);
            throw new IllegalStateException(e);
        }

        int secsBetweenRuns = 5;
        for (long sec = 0; sec <= 24 * 60 *60; sec += secsBetweenRuns){
            startRuns(10, secsBetweenRuns);
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
        String entryPointAddress = entryPointAddresses.get(entryPointId);
        
        // Point the client to the entry point
        WebTarget webTarget = client.target(entryPointAddress).path(EP_PATH).path(EP_SERVICE_PATH).path("User" + random.nextLong());
        String response = webTarget.request(MediaType.APPLICATION_JSON).get(String.class);
        
        // current time after the response
        long endTime = System.currentTimeMillis();        
        
    }
    
    private static void sleep(double secs) {
        try {
            Thread.sleep(Math.round(secs * 1000));
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
