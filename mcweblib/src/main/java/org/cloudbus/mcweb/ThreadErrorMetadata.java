package org.cloudbus.mcweb;

import com.google.common.base.Preconditions;

/**
 * 
 * Represents meta-information about an unhandled throwable in a thread, without
 * holding a reference to the thread.
 * 
 * @author nikolay.grozev
 *
 */
public class ThreadErrorMetadata {

    private final Throwable error;
    private final long threadID;
    private final String threadName;
    private final long timeOfError;

    /**
     * Constructor.
     * 
     * @param error
     *            - the error, which occured. Must not be null.
     * @param thread
     *            - the thread from which the error ocurred. Must not be null.
     */
    public ThreadErrorMetadata(final Throwable error, final Thread thread) {
        Preconditions.checkNotNull(error);
        Preconditions.checkNotNull(thread);

        this.error = error;
        this.threadID = thread.getId();
        this.threadName = thread.getName();
        this.timeOfError = System.currentTimeMillis();
    }

    /**
     * Returns the error.
     * 
     * @return the error.
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Returns the Id of the thread.
     * 
     * @return the thread id
     */
    public long getThreadID() {
        return threadID;
    }

    /**
     * The thread name.
     * 
     * @return the name of the thread.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * The time of the error in millis, as per {@link System.currentTimeMillis}.
     * 
     * @return the time of the error in millis, as per
     *         {@link System.currentTimeMillis}.
     */
    public long getTimeOfError() {
        return timeOfError;
    }

}
