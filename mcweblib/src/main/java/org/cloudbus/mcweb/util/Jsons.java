package org.cloudbus.mcweb.util;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

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
     * Converts a list to Json.
     * 
     * @param src
     *            - the list of elements. Must not be null.
     * @param clazz
     *            - the superclass of the elements in the list, whose properties
     *            to include in the json.
     * @return the json representation of the list.
     */
    public static <T> String toJson(final List<? extends T> src, final Class<T> clazz) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(clazz);
        
        // TODO For some reason the following code does not work. It includes all
        // properties, not only the one from clazz
        /*
         * Type listType = new TypeToken<List<T>>() { }.getType(); 
         * return GSON.toJson(src, listType);
         */
        
        // Therefore we need this workaround:
        List<T> converted = src.stream().map(e -> fromJson(toJson(e, clazz), clazz)).collect(Collectors.toList());
        return GSON.toJson(converted);
    }
    
    /**
     * Converts a list to Json.
     * 
     * @param src
     *            - the list of elements. Must not be null.
     * @param clazz
     *            - the superclass of the elements in the list, whose properties
     *            to include in the json.
     * @param appendable
     *            - where to write the results to. Must not be null.
     * @return the json representation of the list.
     */
    public static <T> void toJson(final List<? extends T> src, final Class<T> clazz, final Appendable appendable) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(appendable);
        
        // TODO For some reason the following code does not work. It includes all
        // properties, not only the one from clazz
        /*
         * Type listType = new TypeToken<List<T>>() { }.getType(); 
         * return GSON.toJson(src, listType, appendable);
         */
        
        // Therefore we need this workaround:
        try {
            appendable.append(toJson(src, clazz));
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
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
     * Loads a list of objects from the json string.
     * 
     * @param json
     *            - the json string. Must not be null;
     * @param clazz
     *            - the rclass of the elements in the list, whose properties to
     *            include in the json.
     * @return the loaded list
     */
    public static <T> List<T> listFromJson(final String json, final Class<T> clazz) {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(clazz);
        
        Type listType = new TypeToken<List<T>>() { }.getType();
        return GSON.fromJson(json, listType);
    }
    
}
