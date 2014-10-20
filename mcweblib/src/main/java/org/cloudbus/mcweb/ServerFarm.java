package org.cloudbus.mcweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Represents a set/farm of servers.
 * 
 * @author nikolay.grozev
 */
public class ServerFarm {

    private final List<VirtualMachine> servers;

    /**
     * Constr.
     * 
     * @param servers
     *            - the set of servers. Must not be null. Must not be empty.
     */
    public ServerFarm(final List<VirtualMachine> servers) {
        Preconditions.checkNotNull(servers);
        Preconditions.checkArgument(!servers.isEmpty());

        this.servers = new ArrayList<>(servers);
    }

    /**
     * Returns the servers in this farm.
     * @return the servers in this farm.
     */
    public List<VirtualMachine> getServers() {
        return Collections.unmodifiableList(servers);
    }
    
    /**
     * Adds a new server to the farm.
     * @param vm - the new server. Must not be null.
     */
    public void addServer(final VirtualMachine vm){
        Preconditions.checkNotNull(vm);
        servers.add(vm);
    }
}
