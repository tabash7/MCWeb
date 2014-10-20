package org.cloudbus.mcweb;

import com.google.common.base.Preconditions;

/**
 * 
 * @author nikolay.grozev
 */
public class VMType {

    private final String identifier;
    private final double costPerMinute;
    private final double ramInMegabytes;
    private final double noramalisedCPUCapacity;

    /**
     * Constructor.
     * 
     * @param identifier
     *            - a unique textual identifier of the VM type. Must not be
     *            null.
     * @param costPerMinute
     *            - an estimation of the cost per minute. Must not be negative.
     * @param ramInMegabytes
     *            - an estimation of the RAM in megabytes. Must be positive.
     * @param noramalisedCPUCapacity
     *            - an estimation of the noramalised CPU capacity. Must be in
     *            the interval (0,1]
     */
    public VMType(final String identifier,
            final double costPerMinute,
            final double ramInMegabytes,
            final double noramalisedCPUCapacity) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkArgument(costPerMinute >= 0);
        Preconditions.checkArgument(ramInMegabytes > 0);
        Preconditions.checkArgument(noramalisedCPUCapacity > 0);
        Preconditions.checkArgument(noramalisedCPUCapacity <= 1);
        
        this.identifier = identifier;
        this.costPerMinute = costPerMinute;
        this.ramInMegabytes = ramInMegabytes;
        this.noramalisedCPUCapacity = noramalisedCPUCapacity;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getCostPerMinute() {
        return costPerMinute;
    }

    public double getRamInMegabytes() {
        return ramInMegabytes;
    }

    public double getNoramalisedCPUCapacity() {
        return noramalisedCPUCapacity;
    }
}
