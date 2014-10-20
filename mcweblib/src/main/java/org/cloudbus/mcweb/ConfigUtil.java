package org.cloudbus.mcweb;

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

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;

/**
 * Utility class for parsing configuration files.
 * 
 * @author nikolay.grozev
 *
 */
public final class ConfigUtil {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ConfigUtil.class.getCanonicalName());

    /** The separator in the csv file. */
    public static final char CSV_SEP = ';';
    /** The quote symbol in the csv and tsv files. */
    public static final char QUOTE_SYMBOL = '\"';

    /** Suppress instantiation. */
    private ConfigUtil() {
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
