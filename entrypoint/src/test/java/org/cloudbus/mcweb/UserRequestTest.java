package org.cloudbus.mcweb;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.LookUpGeoLocationService;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class UserRequestTest {

    @Test
    public void testAllCloudsHaveLatencyBelowSLA() {
        // Create user request
        UserRequest req = new UserRequest("127.0.0.0", "nik");

        // Create cloud sites
        CloudSite cs1 = new CloudSite("CS1", "127.0.0.1", "127.0.0.1");
        CloudSite cs2 = new CloudSite("CS2", "127.0.0.2", "127.0.0.2");
        CloudSite cs3 = new CloudSite("CS3", "127.0.0.3", "127.0.0.3");
        CloudSite cs4 = new CloudSite("CS3", "127.0.0.4", "127.0.0.4");

        // Create a look-up geo-location service
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + cs1.getIPAddress(), 2d,
                req.getIpAddress() + cs2.getIPAddress(), 5d, 
                req.getIpAddress() + cs3.getIPAddress(), 3d,
                req.getIpAddress() + cs4.getIPAddress(), 4d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Finish up the initialisation of the request
        req.setLatencySLA(10);
        req.setGeoLocationService(geoLocationService);

        // Set up the response from each cloud site, them to the 
        // user request and validate the state
        CloudSiteResponse cs1Response = new CloudSiteResponse(true, 20, cs1);
        CloudSiteResponse cs2Response = new CloudSiteResponse(true, 15, cs2);
        CloudSiteResponse cs3Response = new CloudSiteResponse(false, 10, cs3);
        CloudSiteResponse cs4Response = new CloudSiteResponse(true, 20, cs4);
        
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs1Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 1);
        
        req.addResponseFromCloudSite(cs2Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);

        req.addResponseFromCloudSite(cs3Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);
        
        req.addResponseFromCloudSite(cs4Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 3);
        
        // Get the final selection of a cloud site - must be cs2
        CloudSite cs = req.selectCloudSite();
        assertEquals(cs2, cs);
    }

    @Test
    public void testAllCloudsHaveLatencyAboveSLA() {
        // Create user request
        UserRequest req = new UserRequest("127.0.0.0", "nik");

        // Create cloud sites
        CloudSite cs1 = new CloudSite("CS1", "127.0.0.1", "127.0.0.1");
        CloudSite cs2 = new CloudSite("CS2", "127.0.0.2", "127.0.0.2");
        CloudSite cs3 = new CloudSite("CS3", "127.0.0.3", "127.0.0.3");
        CloudSite cs4 = new CloudSite("CS3", "127.0.0.4", "127.0.0.4");

        // Create a look-up geo-location service
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + cs1.getIPAddress(), 20d,
                req.getIpAddress() + cs2.getIPAddress(), 50d, 
                req.getIpAddress() + cs3.getIPAddress(), 15d,
                req.getIpAddress() + cs4.getIPAddress(), 40d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Finish up the initialisation of the request
        req.setLatencySLA(10);
        req.setGeoLocationService(geoLocationService);

        // Set up the response from each cloud site, them to the 
        // user request and validate the state
        CloudSiteResponse cs1Response = new CloudSiteResponse(true, 20, cs1);
        CloudSiteResponse cs2Response = new CloudSiteResponse(true, 15, cs2);
        CloudSiteResponse cs3Response = new CloudSiteResponse(false, 10, cs3);
        CloudSiteResponse cs4Response = new CloudSiteResponse(true, 20, cs4);
        
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs1Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 1);
        
        req.addResponseFromCloudSite(cs2Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);

        req.addResponseFromCloudSite(cs3Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);
        
        req.addResponseFromCloudSite(cs4Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 3);
        
        // Get the final selection of a cloud site - must be cs2
        CloudSite cs = req.selectCloudSite();
        assertEquals(cs1, cs);
    }
    
    @Test
    public void testCloudsBelowAndAboveLatencySLA() {
        // Create user request
        UserRequest req = new UserRequest("127.0.0.0", "nik");

        // Create cloud sites
        CloudSite cs1 = new CloudSite("CS1", "127.0.0.1", "127.0.0.1");
        CloudSite cs2 = new CloudSite("CS2", "127.0.0.2", "127.0.0.2");
        CloudSite cs3 = new CloudSite("CS3", "127.0.0.3", "127.0.0.3");
        CloudSite cs4 = new CloudSite("CS3", "127.0.0.4", "127.0.0.4");

        // Create a look-up geo-location service
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + cs1.getIPAddress(), 5d,
                req.getIpAddress() + cs2.getIPAddress(), 70d, 
                req.getIpAddress() + cs3.getIPAddress(), 15d,
                req.getIpAddress() + cs4.getIPAddress(), 8d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Finish up the initialisation of the request
        req.setLatencySLA(10);
        req.setGeoLocationService(geoLocationService);

        // Set up the response from each cloud site, them to the 
        // user request and validate the state
        CloudSiteResponse cs1Response = new CloudSiteResponse(true, 20, cs1);
        CloudSiteResponse cs2Response = new CloudSiteResponse(true, 15, cs2);
        CloudSiteResponse cs3Response = new CloudSiteResponse(false, 10, cs3);
        CloudSiteResponse cs4Response = new CloudSiteResponse(true, 19, cs4);
        
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs1Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 1);
        
        req.addResponseFromCloudSite(cs2Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);

        req.addResponseFromCloudSite(cs3Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 2);
        
        req.addResponseFromCloudSite(cs4Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 3);
        
        // Get the final selection of a cloud site - must be cs2
        CloudSite cs = req.selectCloudSite();
        assertEquals(cs4, cs);
    }
    
    
    @Test
    public void testSingleCloud() {
        // Create user request
        UserRequest req = new UserRequest("127.0.0.0", "nik");

        // Create cloud sites
        CloudSite cs1 = new CloudSite("CS1", "127.0.0.1", "127.0.0.1");

        // Create a look-up geo-location service
        Map<String, Double> latencyCache = ImmutableMap.of(req.getIpAddress() + cs1.getIPAddress(), 2d);
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);

        // Finish up the initialisation of the request
        req.setLatencySLA(10);
        req.setGeoLocationService(geoLocationService);

        // Set up the response from each cloud site, them to the 
        // user request and validate the state
        CloudSiteResponse cs1Response = new CloudSiteResponse(true, 20, cs1);
        
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs1Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 1);
        
        // Get the final selection of a cloud site - must be cs1
        CloudSite cs = req.selectCloudSite();
        assertEquals(cs1, cs);
    }
    
    @Test
    public void testAllUneligible() {
        // Create user request
        UserRequest req = new UserRequest("127.0.0.0", "nik");

        // Create cloud sites
        CloudSite cs1 = new CloudSite("CS1", "127.0.0.1", "127.0.0.1");
        CloudSite cs2 = new CloudSite("CS2", "127.0.0.2", "127.0.0.2");
        CloudSite cs3 = new CloudSite("CS3", "127.0.0.3", "127.0.0.3");

        // Create a look-up geo-location service
        Map<String, Double> latencyCache = ImmutableMap.of();
        IGeolocationService geoLocationService = new LookUpGeoLocationService(null, latencyCache, null);
        
        // Finish up the initialisation of the request
        req.setLatencySLA(10);
        req.setGeoLocationService(geoLocationService);

        // Set up the response from each cloud site, them to the 
        // user request and validate the state
        CloudSiteResponse cs1Response = new CloudSiteResponse(false, 20, cs1);
        CloudSiteResponse cs2Response = new CloudSiteResponse(false, 15, cs2);
        CloudSiteResponse cs3Response = new CloudSiteResponse(false, 10, cs3);
        
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs1Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        req.addResponseFromCloudSite(cs2Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);

        req.addResponseFromCloudSite(cs3Response);
        validateViableCloudSiteResponses(req.viableCloudSiteResponses, 0);
        
        // Get the final selection of a cloud site - must be null
        CloudSite cs = req.selectCloudSite();
        assertNull(cs);
    }
    
    private static void validateViableCloudSiteResponses(List<CloudSiteResponse> responses, int expectedSize) {
        assertEquals(expectedSize, responses.size());
        if (responses.size() > 1) {
            for (int i = 0; i < responses.size() - 1; i++) {
                CloudSiteResponse curr = responses.get(i);
                CloudSiteResponse next = responses.get(i+1);
                assertTrue(curr.isEligible());
                assertTrue(next.isEligible());
                
                if(curr.getCostEstimation() > next.getCostEstimation()) {
                    fail("Responses list is not sorted at positions: " + i + " - " + (i+1));
                }
            }
        }
    }
}
