package org.cloudbus.mcweb;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class CloudSiteTest {

    @Test
    public void testConstruction() throws UnknownHostException {
        String loadBalancerURL = "example.com";
        String loadBalancerAddress = "https://" + loadBalancerURL + "/path/to/my/service?user=me&pass=secret";
        @SuppressWarnings("resource")
        CloudSite site = new CloudSite("test", "127.0.0.1/test/service", loadBalancerAddress);
        assertEquals(InetAddress.getByName(loadBalancerURL).getHostAddress(), site.getIPAddress());
    }
    
    @Test(expected = RuntimeException.class)
    public void testFailedConstruction() throws UnknownHostException {
        String loadBalancerURL = "xxx.yy.nonexistent.strange.one.example.comaubg";
        String loadBalancerAddress = "https://" + loadBalancerURL + "/path/to/my/service?user=me&pass=secret";
        @SuppressWarnings({ "unused", "resource" })
        CloudSite site = new CloudSite("test", "127.0.0.1/test/service", loadBalancerAddress);
    }
}
