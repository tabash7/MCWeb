package org.cloudbus.mcweb.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudbus.mcweb.VMType;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;

/**
 * Utility class for parsing configuration files, and with common constants.
 * 
 * @author nikolay.grozev
 *
 */
public final class Configs {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(Configs.class.getCanonicalName());

    /* --- Web services paths --- */
    /** Service path. */
    public static final String SERVICE_PATH = "/service";

    public static final int DEFAULT_EP_PORT = 8080;
    public static final String EP_PATH = "/entry-point";
    public static final String SOURCE_IP_PARAM = "sourceIP";
    public static final String USER_TOKEN_PARAM = "userToken";
    public static final String EP_SERVICE_PATH = SERVICE_PATH + 
            "/{" + SOURCE_IP_PARAM + "}/" +
            "{" + USER_TOKEN_PARAM + "}";

    public static final int DEFAULT_AC_PORT = DEFAULT_EP_PORT + 1;
    public static final String AC_PATH = "/admission-control";
    public static final String USER_TOKENS_PARAM = "userTokens";
    public static final String AC_SERVICE_PATH = SERVICE_PATH;
    
    /* --- CSV constants --- */
    /** The separator in the csv file. */
    public static final char CSV_SEP = ';';
    /** The quote symbol in the csv and tsv files. */
    public static final char QUOTE_SYMBOL = '\"';

    /** Suppress instantiation. */
    private Configs() {
    }

    public static List<VMType> parseVMTypes(final InputStream inStream) {
        Preconditions.checkNotNull(inStream);

        Set<String> identifiers = new HashSet<String>();
        
        List<VMType> sites = new ArrayList<>();
        try (InputStream stream = inStream;
                InputStreamReader reader = new InputStreamReader(stream);
                CSVReader csv = new CSVReader(reader, CSV_SEP, QUOTE_SYMBOL)) {

            Preconditions.checkArgument(inStream.available() > 0, "No data in the VM types config.");

            // Read the file line by line
            // Skip the header
            String[] lineElems = csv.readNext();

            while ((lineElems = csv.readNext()) != null) {
                for (int i = 0; i < lineElems.length; i++) {
                    lineElems[i] = lineElems[i].trim();
                }
                
                if (identifiers.contains(lineElems[0])) {
                    throw new IllegalStateException("Duplicate VM type identifier: " + lineElems[0]);
                }
                
                identifiers.add(lineElems[0]);
                sites.add(new VMType(lineElems[0], 
                        Double.parseDouble(lineElems[1]), 
                        Double.parseDouble(lineElems[2]),
                        Double.parseDouble(lineElems[3])));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load the cloud sites!", e);
            throw new IllegalStateException(e);
        }
        return sites;
    }
    
    /**
     * Creates an InputStream from a string.
     * @param s - the string to use. Must not be null.
     * @return - an input stream to the string.
     */
    public static InputStream streamFrom(final String s) {
        Preconditions.checkNotNull(s);
        return new ByteArrayInputStream(s.getBytes());
    }
}
