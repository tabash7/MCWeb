package org.cloudbus.mcweb;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;

/**
 * Represents a set/farm of servers.
 * 
 * @author nikolay.grozev
 */
public class ServerFarm implements AutoCloseable {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ServerFarm.class.getCanonicalName());
    
    /** VMs indexed by the addresses. */
    private final LinkedHashMap<String, VirtualMachine> servers;
    /** The last cost estimation for serving a user per minute. */
    private double latestCostEstimation = 0;

    /** ThreadPool for connecting fetching VM utilisations. */
    private final ExecutorService cloudSitesThreadPool = Executors.newCachedThreadPool();
    /** Period between VM utilisation fetching in millis. Must be positive. */
    private final long periodBetweenVMUtilFetching;
    
    /** Periodically fetches utilisation from the VMs. */
    private final Timer bacthRequestTimer = new Timer("VM Utilisation Batch Timer", true);
    /** The thread, fetching . */
    private final TimerTask bacthRequestTimerTask = new TimerTask() {
        @Override
        public void run() {
            fetch();
        }
    };

    /**
     * Constr.
     * 
     * @param servers
     *            - the set of servers. Must not be null. Must not be empty.
     *            None of the elements must be null.
     * @param periodBetweenVMUtilFetching
     *            - period between VM utilisation fetching in milliseconds. Must
     *            be positive.
     */
    public ServerFarm(final List<VirtualMachine> servers, final long periodBetweenVMUtilFetching) {
        Preconditions.checkNotNull(servers);
        Preconditions.checkArgument(!servers.isEmpty());
        Preconditions.checkArgument(periodBetweenVMUtilFetching > 0);

        // Index all servers by their address
        this.servers = new LinkedHashMap<>();
        for (VirtualMachine vm : servers) {
            Preconditions.checkNotNull(vm);
            Preconditions.checkArgument(!this.servers.containsKey(vm.getAddress()), "Address %s is duplicated", vm.getAddress());
            
            this.servers.put(vm.getAddress(), vm);
        }

        this.periodBetweenVMUtilFetching = periodBetweenVMUtilFetching;
        
        // Start the background thread, fetching VMs utilisations
        bacthRequestTimer.schedule(bacthRequestTimerTask, 10, this.periodBetweenVMUtilFetching);
    }

    /**
     * Returns the servers in this farm.
     * 
     * @return the servers in this farm.
     */
    public synchronized Collection<VirtualMachine> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    /**
     * Adds a new server to the farm.
     * 
     * @param vm
     *            - the new server. There must not be a registered server with
     *            the same address. Must not be null.
     */
    public synchronized void addServer(final VirtualMachine vm) {
        Preconditions.checkNotNull(vm);
        Preconditions.checkArgument(!servers.containsKey(vm.getAddress()));
        servers.put(vm.getAddress(), vm);
    }

    /**
     * Removes the server with the specified address if present in the farm.
     * 
     * @param serverAddress
     *            - the address of the server to remove. Must not be null.
     * @return the removed virtual machine, if any. If a server with this
     *         address was not present, the method returns null.
     */
    public synchronized VirtualMachine removeServer(final String serverAddress) {
        Preconditions.checkNotNull(serverAddress);
        return servers.remove(serverAddress);
    }

    /**
     * Returns the estimation for serving a user per minute in this server farm. 
     * @return the estimation for serving a user per minute in this server farm.
     */
    public strictfp synchronized double costPerUser() {
        int count = 0;
        double accummulatedCost = 0;
        for (VirtualMachine vm : servers.values()) {
            double vmCostPerUser = vm.costPerUser();
            if (!Double.isNaN(vmCostPerUser)) {
                count++;
                accummulatedCost += vmCostPerUser;
            }
        }
        
        if (count != 0) {
            latestCostEstimation = accummulatedCost / count;
            return latestCostEstimation;
        }
        
        LOG.log(Level.INFO, "Estimated the cost: {0}", new Object[]{latestCostEstimation});
        
        return latestCostEstimation;
    }
    
    /**
     * Returns the server associated with the specified address. Otherwise -
     * null.
     * 
     * @param serverAddress
     *            - the address of the server to retrieve. Must not be null.
     * @return the virtual machine with the address if present. Otherwise null.
     */
    synchronized VirtualMachine getServer(final String serverAddress) {
        Preconditions.checkNotNull(serverAddress);
        return servers.get(serverAddress);
    }
    
    synchronized long getPeriodBetweenVMUtilFetching() {
        return periodBetweenVMUtilFetching;
    }

    private synchronized void fetch() {
        for (VirtualMachine vm : servers.values()) {
            LOG.log(Level.INFO, "Fetching data from server {0}", new Object[]{vm});
            cloudSitesThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    vm.fetch();
                }
            });
        }
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.log(Level.INFO, "Closing {0}", new Object[]{this});
        
        Closer closer = Closer.create();
        
        // Stop all cloud sites
        for (VirtualMachine vm : servers.values()) {
            closer.register(() -> {
                try {
                    vm.close();   
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
