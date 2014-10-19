package org.cloudbus.mcweb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;

/**
 * Utility class for parsing the configuration files.
 * 
 * @author nikolay.grozev
 *
 */
public final class ConfigUtil {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(ConfigUtil.class.getCanonicalName());

    /** Config prop key. */
    public static final String LATENCY_SLA_PROP = "latenctSLA";
    /** Config prop key. */
    public static final String PERIOD_BETWEEN_BATCH_USER_DISPATCH_PROP = "periodBetweenBatchUserDispatch";
    /** Config prop key. */
    public static final String CLOUD_SITE_RESPONSE_TIMEOUT_PROP = "cloudSiteResponseTimeout";
    /** Config prop key. */
    public static final String MAX_REQUEST_PERIOD_PROP = "maxRequestPeriod";
    /** The separator in the csv file. */
    public static final char CSV_SEP = ';';
    /** The quote symbol in the csv and tsv files. */
    public static final char QUOTE_SYMBOL = '\"';

    /** Suppress instantiation. */
    private ConfigUtil() {
    }

    public static Properties parseConfig(final InputStream inStream) {
        Preconditions.checkNotNull(inStream);
        Properties properties = new Properties();
        try (InputStream stream = inStream) {
            Preconditions.checkArgument(inStream.available() > 0, "No data in the constraints config.");

            properties.load(stream);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load the configuration!", e);
            throw new IllegalStateException(e);
        }
        return properties;
    }

    /**
     * Reads the cloudsite definitions. Closes the stream.
     * 
     * @param inStream
     *            - the stream to read from. Must be in the valid format. Will
     *            be closed as a result.
     * @param cloudSiteFactory
     *            - a factory function, which creates a CloudSite, based on an
     *            array of [name, admissionControllerAddress,
     *            loadBalancerAddress].Must not be null.
     */
    public static List<CloudSite> parseCloudSites(final InputStream inStream,
            final Function<String[], CloudSite> cloudSiteFactory) {
        Preconditions.checkNotNull(inStream);
        Preconditions.checkNotNull(cloudSiteFactory);

        List<CloudSite> sites = new ArrayList<>();
        try (InputStream stream = inStream;
                InputStreamReader reader = new InputStreamReader(stream);
                CSVReader csv = new CSVReader(reader, CSV_SEP, QUOTE_SYMBOL)) {

            Preconditions.checkArgument(inStream.available() > 0, "No data in the cloud sites config.");

            // Read the file line by line
            // Skip the header
            String[] lineElems = csv.readNext();


            while ((lineElems = csv.readNext()) != null) {
                for (int i = 0; i < lineElems.length; i++) {
                    lineElems[i] = lineElems[i].trim();
                }
                sites.add(cloudSiteFactory.apply(lineElems));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not load the cloud sites!", e);
            throw new IllegalStateException(e);
        }
        return sites;
    }
}
