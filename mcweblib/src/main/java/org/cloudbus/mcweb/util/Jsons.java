package org.cloudbus.mcweb.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A utility class for working with Json. It wraps the Gson library.
 * 
 * @author nikolay.grozev
 */
public class Jsons {

    /** Suppress construction. */
    private Jsons() {
    }

    /** The wrapped Gson. */
    private static final Gson GSON = new GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .serializeNulls()
        .setPrettyPrinting().create();

    /**
     * Serialises to Json.
     * 
     * @param src
     *            - the object to serialise. Must not be null.
     * @return the serialised representation of the object.
     */
    public static String toJson(final Object src) {
        Preconditions.checkNotNull(src);
        return GSON.toJson(src);
    }

    /**
     * Serialises to Json in the appendable buffer.
     * 
     * @param src
     *            - the object to serialise. Must not be null.
     * @param appendable
     *            - where to write the results to. Must not be null.
     * @return the serialised representation of the object.
     */
    public static void toJson(final Object src, final Appendable appendable) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(appendable);
        GSON.toJson(src, appendable);
    }
    
    /**
     * Serialises to Json, as if it was an instance of its specified superclass.
     * 
     * @param src
     *            - the object to serialise. Must not be null.
     * @param class
     *            - the class to serialise to. Must not be null.
     * @return the serialised representation of the object.
     */
    public static <T> String toJson(final T src, final Class<T> clazz) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(clazz);
        return GSON.toJson(src, clazz);
    } 
    
    /**
     * Serialises to Json, as if it was an instance of its specified superclass.
     * 
     * @param src
     *            - the object to serialise. Must not be null.
     * @param class
     *            - the class to serialise to. Must not be null.
     * @param appendable
     *            - where to write the results to. Must not be null.
     * @return the serialised representation of the object.
     */
    public static <T> void toJson(final T src, final Class<T> clazz, final Appendable appendable) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(appendable);
        GSON.toJson(src, clazz, appendable);
    } 
    
    /**
     * Loads an object from Json.
     * 
     * @param json
     *            - the json string. Must not be null.
     * @param class
     *            - the expected type of the loaded object. Must not be null.
     * @return the loaded object.
     */
    public static <T> T fromJson(final String json, final Class<T> clazz) {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(clazz);
        return GSON.fromJson(json, clazz);
    }
    
    /**
     * Loads an object from Json.
     * 
     * @param json
     *            - the json reader. Must not be null.
     * @param class
     *            - the expected type of the loaded object. Must not be null.
     * @return the loaded object.
     */
    public static <T> T fromJson(final Reader json, final Class<T> clazz) {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(clazz);
        return GSON.fromJson(json, clazz);
    }

    /**
     * Loads an object from Json.
     * 
     * @param json
     *            - the json input stream. Must not be null.
     * @param class
     *            - the expected type of the loaded object. Must not be null.
     * @return the loaded object.
     */
    public static <T> T fromJson(final InputStream json, final Class<T> clazz) {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(clazz);
        return GSON.fromJson(new InputStreamReader(json), clazz);
    }
}
