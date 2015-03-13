package org.cloudbus.mcweb.entrypoint;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static org.cloudbus.mcweb.util.Configs.*;

@Path(EP_PATH)
public class EntryPointService {
    
    @GET
    @Path(EP_SERVICE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String service(@PathParam(SOURCE_IP_PARAM) final String sourceIP, @PathParam(USER_TOKEN_PARAM) final String userToken) {
        EPUserRequest req = new EPUserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();

        return Objects.toString(redirectAddress);
    }
    
}