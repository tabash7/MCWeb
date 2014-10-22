package org.cloudbus.mcweb;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a VM from the web server farm.
 * 
 * @author nikolay.grozev
 */
public class VirtualMachine implements AutoCloseable {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(VirtualMachine.class.getCanonicalName());
    
    private final String address;
    private final VMType type;

    private double cpuUtil;
    private double ramUtil;
    private int numUsers;

    private double lastCost = Double.NaN;

    /**
     * Constructor.
     * 
     * @param address
     *            - the address of the VM. Can be a DNS or IP address. Can be
     *            used to SSH or RDP the VM. Must not be null.
     * @param type
     */
    public VirtualMachine(final String address, final VMType type) {
        Preconditions.checkNotNull(address);
        Preconditions.checkNotNull(type);

        this.address = address;
        this.type = type;
    }

    /**
     * Returns the address of the VM.
     * 
     * @return the address of the VM.
     */
    public synchronized String getAddress() {
        return address;
    }

    /**
     * Returns the VM type.
     * 
     * @return the VM type
     */
    public synchronized VMType getType() {
        return type;
    }

    /**
     * Returns the CPU utilisation as a number in the interval (0;1].
     * 
     * @return the CPU utilisation as a number in the interval (0;1].
     */
    public synchronized double getCPUUtil() {
        return cpuUtil;
    }

    /**
     * Sets the CPU util
     * 
     * @param ramUtil
     *            - the CPU util. Must be in the interval (0;1]
     */
    public synchronized void setCpuUtil(double cpuUtil) {
        Preconditions.checkArgument(cpuUtil > 0);
        Preconditions.checkArgument(cpuUtil <= 1);
        this.cpuUtil = cpuUtil;
    }

    /**
     * Returns the RAM utilisation as a number in the interval (0;1].
     * 
     * @return the RAM utilisation as a number in the interval (0;1].
     */
    public synchronized double getRAMUtil() {
        return ramUtil;
    }

    /**
     * Sets the RAM util
     * 
     * @param ramUtil
     *            - the RAM util. Must be in the interval (0;1]
     */
    public synchronized void setRamUtil(double ramUtil) {
        Preconditions.checkArgument(ramUtil > 0);
        Preconditions.checkArgument(ramUtil <= 1);
        this.ramUtil = ramUtil;
    }

    /**
     * Returns the number of users being served.
     * 
     * @return the number of users being served.
     */
    public synchronized int getNumUsers() {
        return numUsers;
    }

    /**
     * Sets the number of users measurements.
     * 
     * @param numUsers
     *            - the num of usres. Must not be negative.
     */
    protected synchronized void setNumUsers(final int numUsers) {
        Preconditions.checkArgument(numUsers >= 0);
        this.numUsers = numUsers;
    }

    /**
     * Fetches measurements from the VM.
     */
    public synchronized void fetch() {
        LOG.log(Level.INFO, "Fetching data from :{0}", new Object[]{this});
        setCpuUtil(0.001);
        setRamUtil(0.001);
        setNumUsers(0);
    }

    /**
     * Returns an estimation of the cost for serving a user per minute. Returns
     * NaN if the cost cannot be estimated.
     * 
     * @return an estimation of the cost for serving a user per minute or NaN if
     *         it cannot be estimated.
     */
    public strictfp synchronized double costPerUser() {
        if (getNumUsers() > 0) {
            double maxNumberUsers = getNumUsers() / Math.max(getCPUUtil(), getRAMUtil());
            lastCost = getType().getCostPerMinute() / maxNumberUsers;
        }
        LOG.log(Level.INFO, "Estimating the cost for {0}, Num Users: {1}, CPUUtil: {2}, RAMUtil: {3}", 
                new Object[]{this,
                    getNumUsers(),
                    getCPUUtil(),
                    getRAMUtil()});
        
        return lastCost;
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.log(Level.INFO, "Closing {0}", new Object[]{this});
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Address", this.address)
                .add("Type", this.type.getIdentifier())
                .toString();
    }
}
