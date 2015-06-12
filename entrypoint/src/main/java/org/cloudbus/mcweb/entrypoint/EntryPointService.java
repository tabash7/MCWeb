package org.cloudbus.mcweb.entrypoint;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.cloudbus.mcweb.EntryPointResponse;
import org.cloudbus.mcweb.util.Jsons;

import static org.cloudbus.mcweb.util.Configs.*;

@Path(EP_PATH)
public class EntryPointService {
    
    @GET
    @Path(EP_FULL_SERVICE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String service(@PathParam(SOURCE_IP_PARAM) final String sourceIP, @PathParam(USER_TOKEN_PARAM) final String userToken) {
        EPUserRequest req = new EPUserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();

        return Objects.toString(redirectAddress);
    }

    @GET
    @Path(EP_SERVICE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public String service(@Context HttpServletRequest request, @PathParam(USER_TOKEN_PARAM) final String userToken) {
        String sourceIP = EntryPointRedirectServelet.getClientIpAddr(request);
        EPUserRequest req = new EPUserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        //String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();
        
        EntryPointResponse response = new EntryPointResponse(cs == null ? null : cs.getName(), cs == null? null : cs.getLoadBalancerAddress());
        if(cs instanceof RESTCloudSite) {
        	RESTCloudSite rcs = (RESTCloudSite)cs;
        	response = new EntryPointResponse(cs.getName(), cs.getLoadBalancerAddress(), rcs.getDefinition());
        }
        return Jsons.toJson(response);
    }
    
}