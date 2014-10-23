package org.cloudbus.mcweb.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;

/**
 * Utilities for closing resources with as little code as possible.
 * 
 * @author nikolay.grozev
 *
 */
public class Closeables {

    /** Suppress instantiation. */
    private Closeables() {
    }

    /** A dummy closeable, whose close methods does nothing.*/
    public static final Closeable DUMMY_CLOSEABLE = () -> {};
    
    /**
     * Returns the same closeable if the arguement is not null. If null - a
     * closable, whose close method does nothing is returned.
     * 
     * @param closeable
     *            - the closable object.
     * @return the same closeable if the arguement is not null. If null - a
     *         closable, whose close method does nothing is returned.
     */
    public static AutoCloseable maybeCloseable(final AutoCloseable closeable) {
        return closeable != null ? closeable : DUMMY_CLOSEABLE;
    }

    /**
     * Returns the same closeable if the arguement is not null. If null - a
     * closable, whose close method does nothing is returned.
     * 
     * @param closeable
     *            - the closable object.
     * @return the same closeable if the arguement is not null. If null - a
     *         closable, whose close method does nothing is returned.
     */
    public static Closeable maybeCloseable(final Closeable closeable) {
        return closeable != null ? closeable : DUMMY_CLOSEABLE;
    }

    /**
     * Returns the same closeable if both arguements are not null. Otherwise - a
     * closable, whose close method does nothing is returned. Useful, when
     * creating one-liner anonymous/lambda closeables, whose parameter may be
     * null.
     * 
     * @param param
     *            - the parameter to use. May be null
     * @param closeable
     * @return the same closeable if both arguements are not null. Otherwise - a
     *         closable, whose close method does nothing is returned.
     */
    public static Closeable maybeCloseable(final Object param, final Closeable closeable) {
        return param != null && closeable != null ? closeable : DUMMY_CLOSEABLE;
    }
    
    /**
     * Returns the same closeable if both arguements are not null. Otherwise - a
     * closable, whose close method does nothing is returned. Useful, when
     * creating one-liner anonymous/lambda closeables, whose parameter may be
     * null.
     * 
     * @param param
     *            - the parameter to use. May be null
     * @param closeable
     * @return the same closeable if both arguements are not null. Otherwise - a
     *         closable, whose close method does nothing is returned.
     */
    public static AutoCloseable maybeCloseable(final Object param, final AutoCloseable closeable) {
        return param != null && closeable != null ? closeable : DUMMY_CLOSEABLE;
    }
    
    
    /**
     * If the parameter is not null, returns a closeable which invokes notifyAll
     * on its lock. Otherwise (i.e. if null) returns a closeable, which does
     * nothing.
     * 
     * @param lock - the lock
     * @return a closeable which invokes notifyAll on its lock. If the parameter
     *         is null, returns a closeable, which does nothing.
     */
    public static Closeable notifyAllCloseable(final Object lock) {
        return maybeCloseable(lock, () -> {
            synchronized (lock) {
                lock.notify();
            }
        });
    }
    
    /**
     * Converts between {@link AutoCloseable} and {@link Closeable}, as some
     * legacy APIs work with {@link Closeable} only.
     * 
     * @param closeable
     *            - the resource to convert. May be null.
     * @return null of the argument is null. Otherwise, returns an
     *         {@link AutoCloseable} wrapper, which also wraps any exception in
     *         an {@link IOException};
     */
    public static Closeable convert(final AutoCloseable closeable) {
        if (closeable == null) {
            return null;
        } else {
            return () -> {
                try {
                    maybeCloseable(closeable).close();
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    } else {
                        throw new IOException(e);
                    }
                }
            };
        }
    }

    /**
     * Attempts to close all non-null closables. Works recursively, so is not
     * suitable for big collections.
     * 
     * @param closeables
     *            - the cloеsables to close. Must not be null, but may contain
     *            nulls.
     * @throws IOException
     *             - if an exception occurred. Only the last exception is
     *             thrown, if there are multiple. Al exceptions are wrapped in IOExceptions.
     */
    public static void closeAll(AutoCloseable... closeables) throws IOException {
        Preconditions.checkNotNull(closeables);
        closeAll(Arrays.asList(closeables));
    }
    
    /**
     * Attempts to close all non-null closables.
     * 
     * @param closeables
     *            - the cloеsables to close. Must not be null, but may contain
     *            nulls.
     * @throws IOException
     *             - if an exception occurred. Only the last exception is
     *             thrown, if there are multiple. Al exceptions are wrapped in IOExceptions.
     */
    public static void closeAll(Iterable<? extends AutoCloseable> closeables) throws IOException {
        Preconditions.checkNotNull(closeables);
        Closer closer = Closer.create();
        
        // Add them all to the closer
        for (AutoCloseable closeable : closeables) {
            closer.register(maybeCloseable(convert(closeable)));
        }
        
        // Close them all
        closer.close();
    }
    
    /**
     * Attempts to close all non-null closables. Works recursively, so is not
     * suitable for big collections.
     * 
     * @param closeables
     *            - the cloеsables to close. Must not be null, but may contain
     *            nulls.
     * @throws Exception
     *             - if an exception occurred. Only the last exception is
     *             thrown, if there are multiple
     */
    public static void closeAllRecursively(Iterable<? extends AutoCloseable> closeables) throws Exception {
        Preconditions.checkNotNull(closeables);
        closeAllRecursively(closeables.iterator());
    }

    private static void closeAllRecursively(Iterator<? extends AutoCloseable> closables) throws Exception {
        if (closables.hasNext()) {
            try {
                maybeCloseable(closables.next()).close();
            } finally {
                closeAllRecursively(closables);
            }
        }
    }
}
