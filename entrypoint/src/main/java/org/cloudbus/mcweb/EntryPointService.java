package org.cloudbus.mcweb;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/entry-point")
public class EntryPointService {

    @GET
    @Path("test/{sourceIP}/{userToken}")
    @Produces(MediaType.TEXT_PLAIN)
    public String test(@PathParam("sourceIP") final String sourceIP, @PathParam("userToken") final String userToken) {
        UserRequest req = new UserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();

        return Objects.toString(redirectAddress);
    }
}