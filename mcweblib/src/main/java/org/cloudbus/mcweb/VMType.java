package org.cloudbus.mcweb;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * 
 * @author nikolay.grozev
 */
public class VMType {

    private final String identifier;
    private final double costPerMinute;
    private final double ramInMegabytes;
    private final double normalisedCPUCapacity;

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
     * @param normalisedCPUCapacity
     *            - an estimation of the noramalised CPU capacity. Must be in
     *            the interval (0,1]
     */
    public VMType(final String identifier, final double costPerMinute, final double ramInMegabytes,
            final double normalisedCPUCapacity) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkArgument(costPerMinute >= 0);
        Preconditions.checkArgument(ramInMegabytes > 0);
        Preconditions.checkArgument(normalisedCPUCapacity > 0);
        Preconditions.checkArgument(normalisedCPUCapacity <= 1);

        this.identifier = identifier;
        this.costPerMinute = costPerMinute;
        this.ramInMegabytes = ramInMegabytes;
        this.normalisedCPUCapacity = normalisedCPUCapacity;
    }

    /**
     * Returns the unique textual identifier of the VM type.
     * 
     * @return the unique textual identifier of the VM type.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the estimation of the cost per minute.
     * 
     * @return the estimation of the cost per minute.
     */
    public double getCostPerMinute() {
        return costPerMinute;
    }

    /**
     * Returns the estimation of the RAM in megabytes.
     * @return the estimation of the RAM in megabytes.
     */
    public double getRamInMegabytes() {
        return ramInMegabytes;
    }

    /**
     * Returns the estimation of the noramalised CPU capacity.
     * @return the estimation of the noramalised CPU capacity.
     */
    public double getNormalisedCPUCapacity() {
        return normalisedCPUCapacity;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass()).
                add("Id", this.identifier)
                .add("Cost/min", this.costPerMinute)
                .add("RAM/MGB", this.ramInMegabytes)
                .add("NormCPU", this.normalisedCPUCapacity)
                .toString();
    }
}
