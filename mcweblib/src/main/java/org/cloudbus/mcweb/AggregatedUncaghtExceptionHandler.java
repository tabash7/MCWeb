package org.cloudbus.mcweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

/**
 * An {@link Thread.UncaughtExceptionHandler}, which aggregates the errors of
 * the associated threads in a list. Useful for multi-threaded unit tests.
 * 
 * @author nikolay.grozev
 *
 */
public class AggregatedUncaghtExceptionHandler implements Thread.UncaughtExceptionHandler {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(AggregatedUncaghtExceptionHandler.class.getCanonicalName());

    private final List<ThreadErrorMetadata> errors = new ArrayList<>();

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        Preconditions.checkNotNull(e);
        Preconditions.checkNotNull(t);
        
        synchronized (errors) {
            LOG.log(Level.SEVERE, "Error occured in Thread: " + t.getId(), e);
            errors.add(new ThreadErrorMetadata(e, t));
        }
    }

    /**
     * Returns the list of errors, that occurred in the associated threads.
     * 
     * @return the list of errors, that occurred in the associated threads.
     */
    public List<ThreadErrorMetadata> getErrors() {
        synchronized (errors) {
            return Collections.unmodifiableList(errors);
        }
    }
    
    /**
     * Throws the first error, if there is such. Otherwise - does not do
     * anythin.
     * 
     * @throws Throwable
     *             - when at least one of the assciated threads resulted in an
     *             unhandled error.
     */
    public void throwFirst() throws Throwable {
        synchronized (errors) {
            if (!errors.isEmpty()) {
                throw errors.get(0).getError();
            }
        }
    }
}
