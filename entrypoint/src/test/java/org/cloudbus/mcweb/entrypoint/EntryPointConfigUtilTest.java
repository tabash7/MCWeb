package org.cloudbus.mcweb.entrypoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.cloudbus.mcweb.entrypoint.CloudSite;
import org.cloudbus.mcweb.entrypoint.EntryPointConfigUtil;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.cloudbus.mcweb.entrypoint.CloudSite.*;
import static org.cloudbus.mcweb.ConfigUtil.*;
import static org.cloudbus.mcweb.entrypoint.EntryPointConfigUtil.*;

public class EntryPointConfigUtilTest {

    @Test
    public void testParseConfig() throws IOException {
        // Parse 1 cloud site
        InputStream is = streamFrom("Name;      AdmissionControllerAddress;     LoadBalancerAddress\n"
                + "AWS1;    127.0.0.1;                                      127.0.0.1:80\n");
        List<CloudSite> sites = EntryPointConfigUtil.parseCloudSites(is, FACTORY);
        assertEquals(0, is.available());
        assertEquals(1, sites.size());
        assertEquals("AWS1", sites.get(0).getName());
        assertEquals("127.0.0.1", sites.get(0).getAdmissionControllerAddress());
        assertEquals("127.0.0.1:80", sites.get(0).getLoadBalancerAddress());

        // Parse 2 cloud sites
        is = streamFrom("Name;      AdmissionControllerAddress;     LoadBalancerAddress\n"
                + "AWS1;    127.0.0.1;                                      127.0.0.1:80\n"
                + "AWS2;   127.0.0.2;                                      127.0.0.2");
        sites = parseCloudSites(is, FACTORY);
        assertEquals(0, is.available());
        assertEquals(2, sites.size());
        assertEquals("AWS1", sites.get(0).getName());
        assertEquals("127.0.0.1", sites.get(0).getAdmissionControllerAddress());
        assertEquals("127.0.0.1:80", sites.get(0).getLoadBalancerAddress());
        assertEquals("AWS2", sites.get(1).getName());
        assertEquals("127.0.0.2", sites.get(1).getAdmissionControllerAddress());
        assertEquals("127.0.0.2", sites.get(1).getLoadBalancerAddress());

        // Parse 0 cloud sites
        is = streamFrom("Name;      AdmissionControllerAddress;     LoadBalancerAddress");
        sites = parseCloudSites(is, FACTORY);
        assertEquals(0, is.available());
        assertTrue(sites.isEmpty());
    }

    @Test
    public void testParseConstraints() throws IOException {
        double delta = 0.1;

        // Test single value
        InputStream is = streamFrom("latenctSLA=40.123\n"
                + "periodBetweenBatchUserDispatch=3000\n"
                + "cloudSiteResponseTimeout=3000\n"
                + "maxRequestPeriod=6000\n");
        Properties props = parseConfig(is);
        assertEquals(40.123, Double.parseDouble(props.getProperty(LATENCY_SLA_PROP)), delta);
        assertEquals(3000, Double.parseDouble(props.getProperty(PERIOD_BETWEEN_BATCH_USER_DISPATCH_PROP)), delta);
        assertEquals(3000, Double.parseDouble(props.getProperty(CLOUD_SITE_RESPONSE_TIMEOUT_PROP)), delta);
        assertEquals(6000, Double.parseDouble(props.getProperty(MAX_REQUEST_PERIOD_PROP)), delta);
        assertEquals(0, is.available());

        // Test single value
        is = streamFrom("some=more\n" + "latenctSLA=40.123f\n" + "test=value");
        props = parseConfig(is);
        assertEquals(40.123, Double.parseDouble(props.getProperty(LATENCY_SLA_PROP)), delta);
        assertEquals(0, is.available());
    }
}
