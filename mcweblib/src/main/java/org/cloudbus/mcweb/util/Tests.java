package org.cloudbus.mcweb.util;

import com.google.common.base.Preconditions;

/**
 * Common utilities for unit tests.
 * 
 * @author nikolay.grozev
 *
 */
public class Tests {

    private Tests() {
    }

    /**
     * Makes the current thread sleep, without throwing checked exceptions.
     * 
     * @param millis
     *            - how long to sleep. Must not be negative.
     */
    public static void sleep(final long millis) {
        Preconditions.checkArgument(millis >= 0);
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Makes the current thread sleep, without throwing checked exceptions.
     * 
     * @param millis
     *            - how long to sleep. Will be rounded. Must not be negative.
     */
    public static void sleep(final double millis) {
        Preconditions.checkArgument(millis >= 0);
        sleep((long) millis);
    }

}
