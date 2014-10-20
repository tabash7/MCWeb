package org.cloudbus.mcweb;

/**
 * Represents an AS VM from the web server farm.
 * 
 * @author nikolay.grozev
 */
public class VirtualMachine {

    private String address;
    private long pollPeriod;

    public String getAddress() {
        return address;
    }

    public long getPollPeriod() {
        return pollPeriod;
    }

    public double getCPUUtilisation() {
        return 0;
    }

    public double getRAMUtilisation() {
        return 0;
    }

    public double getUserNumber() {
        return 0;
    }

}
