package org.cloudbus.mcweb.entrypoint;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;


@Path("/entry-point")
public class EntryPointService {

    @SuppressWarnings("unused")
    private static final Gson gson = new Gson();
    
    @GET
    @Path("test/{sourceIP}/{userToken}")
    @Produces(MediaType.TEXT_PLAIN)
    public String test(@PathParam("sourceIP") final String sourceIP, @PathParam("userToken") final String userToken) {
        EPUserRequest req = new EPUserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();

        return Objects.toString(redirectAddress);
    }
    
}