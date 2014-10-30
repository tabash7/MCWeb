package org.cloudbus.mcweb.admissioncontroller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.cloudbus.mcweb.AdmissionControllerResponse;

import static org.cloudbus.mcweb.util.Configs.*;

import org.cloudbus.mcweb.util.Jsons;


@Path(AC_PATH)
public class AdmissionControllerService {

    @GET
    @Path(AC_SERVICE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public String test(@QueryParam(USER_TOKENS_PARAM) List<String> userTokens) {
        // userTokens[123, 456]
        List<AdmissionControllerResponse> responses = AdmissionController.getInstance().enquire(userTokens);
        return Jsons.toJson(responses.toArray(new AdmissionControllerResponse[responses.size()]), AdmissionControllerResponse[].class);
    }
}