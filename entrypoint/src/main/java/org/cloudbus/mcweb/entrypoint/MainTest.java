package org.cloudbus.mcweb.entrypoint;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cloudbus.cloudsim.ex.geolocation.IGeolocationService;
import org.cloudbus.cloudsim.ex.geolocation.IPMetadata;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeolocationServiceWithOverrides;
import org.cloudbus.cloudsim.ex.geolocation.geoip2.OverrideRule;
import org.cloudbus.mcweb.util.Jsons;

public class MainTest {

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        GeoIP2PingERService geoIP2PingERService = new GeoIP2PingERService();
        
        
        IGeolocationService service = new GeolocationServiceWithOverrides(geoIP2PingERService, 
                Arrays.asList(Jsons.fromJson(
                        Main.class.getResourceAsStream("/ip-override-rules.json"), 
                        OverrideRule[].class)));
        
        System.out.println(service.latency("52.64.111.150", "115.146.84.159"));
        System.out.println(service.latency("52.64.111.150", "52.68.68.86"));
        /*
         * System.out.println(geoIP2PingERService.getMetaData("52.64.111.150"));
         * System
         * .out.println(geoIP2PingERService.getMetaData("130.56.249.217"));
         */
        Map<String, String> addresses = new LinkedHashMap<String, String>();
        addresses.put("us-east", "54.144.145.48");
        addresses.put("eu-cent", "52.28.146.95");
        addresses.put("apac-tok", "52.68.68.86");
        addresses.put("apac-mel", "115.146.84.159");
        addresses.put("us-west-2", "52.10.33.99");
        addresses.put("us-west-1", "52.8.63.122");
        addresses.put("eu-west", "54.77.55.11");
        addresses.put("apac-si", "54.179.80.21");
        addresses.put("apac-sy", "52.64.111.150");
        addresses.put("sa-bra", "54.94.189.210");

        for (Map.Entry<String, String> e : addresses.entrySet()) {
            IPMetadata md = service.getMetaData(e.getValue());
            System.out.println(e.getKey() + " " +  md + " " + service.getLocationMapUrl(md.getLatitude(), md.getLongitude()));
        }

    }
}
