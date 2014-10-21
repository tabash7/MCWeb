package org.cloudbus.mcweb;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

/**
 * 
 * A VM, whose cost utilisation measurments are predefined. Also has the option
 * to respond to enquiries with a delay.
 * 
 * @author nikolay.grozev
 *
 */
public class PredefinedVirtualMachine extends VirtualMachine {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(PredefinedVirtualMachine.class.getCanonicalName());
    
    private Iterator<Number[]> measurements;
    private final long fetchDelay;

    /**
     * Constructor.
     * 
     * @param address
     *            - see parent.
     * @param type
     *            - see parent.
     * @param measurements
     *            - a sequence of arrays of the type [cpuUtil, ramUtil,
     *            numUsers]. Must not be null or empty. CPU and RAM utils must
     *            be in the interval (0, 1]. User numbers must be non-negative
     *            intergers.
     * @param fetchDelay
     *            - how many milliseconds to delay the fetching of measurements
     *            with. Must not be negative. If 0 - the fetching is not further
     *            delayed.
     */
    public PredefinedVirtualMachine(final String address,
            final VMType type,
            final Iterable<Number[]> measurements,
            final long fetchDelay) {
        super(address, type);
        Preconditions.checkNotNull(measurements);
        Preconditions.checkArgument(measurements.iterator().hasNext());

        // Validate the predefined measurements one by one
        for (Number[] m : measurements) {
            Preconditions.checkArgument(m.length == 3);
            Preconditions.checkArgument(m[2] instanceof Integer);
            Preconditions.checkArgument(m[0].doubleValue() > 0 && m[0].doubleValue() <= 1);
            Preconditions.checkArgument(m[1].doubleValue() > 0 && m[1].doubleValue() <= 1);
            Preconditions.checkArgument(m[2].intValue() >= 0);
        }

        Preconditions.checkArgument(fetchDelay >= 0);
        
        this.measurements = measurements.iterator();
        this.fetchDelay = fetchDelay;
    }

    @Override
    public void fetch() {
        if(fetchDelay > 0) {
            try {
                Thread.sleep(fetchDelay);
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Sleep was interrupted", e);
                throw new IllegalStateException(e);
            }
        }
        
        synchronized (this) {
            super.fetch();
            Number[] next = measurements.next();
    
            setCpuUtil(next[0].doubleValue());
            setRamUtil(next[1].doubleValue());
            setNumUsers(next[2].intValue());
        }
    }
    
    @Override
    public synchronized void close() throws Exception {
        super.close();
        measurements = null;
    }
}
