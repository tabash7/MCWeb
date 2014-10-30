package org.cloudbus.mcweb.entrypoint;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.cloudbus.mcweb.AdmissionControllerResponse;
import org.cloudbus.mcweb.util.Jsons;

import com.google.common.base.Preconditions;

import static org.cloudbus.mcweb.util.Configs.*;
import static org.cloudbus.mcweb.util.Closeables.*;



/**
 * A cloudsite, which uses REST web services to communicate with the admission
 * controller.
 * 
 * @author nikolay.grozev
 *
 */
public class RESTCloudSite extends CloudSite {
    
    private final Client client;
    private final WebTarget webTarget;
    
    
    /**
     * Creates cloud sites which the REST service of the admission controllers.
     */
    public static final Function<String[], CloudSite> FACTORY = s -> new RESTCloudSite(s[0], s[1], s[2]);
    
    /**
     * Constructor.
     * @param name - see superclass.
     * @param admissionControllerAddress - see superclass.
     * @param loadBalancerAddress - see superclass.
     */
    public RESTCloudSite(final String name, final String admissionControllerAddress, final String loadBalancerAddress) {
        super(name, admissionControllerAddress, loadBalancerAddress);

        client = ClientBuilder.newClient();
        webTarget = client.target(getAdmissionControllerAddress()).path(AC_PATH).path(AC_SERVICE_PATH);
    }

    @Override
    public void enquire(List<EPUserRequest> requests) {
        Preconditions.checkNotNull(requests);
        Preconditions.checkArgument(!requests.isEmpty());
        
        List<String> userTokens = requests.stream().map(r -> r.getUserToken()).collect(Collectors.toList());
        String responseJson = webTarget.queryParam(USER_TOKENS_PARAM, userTokens.toArray()).request(MediaType.APPLICATION_JSON).get(String.class);
        AdmissionControllerResponse[] responses = Jsons.fromJson(responseJson, AdmissionControllerResponse[].class);

        // TODO the following could be optimised to avoid nested loops ...
        for (EPUserRequest userRequest : requests) {
            for (AdmissionControllerResponse response : responses){
                if (userRequest.getUserToken().equals(response.getUserToken())) {
                    userRequest.addResponseFromCloudSite(new EPAdmissionControllerResponse(response, this));
                    break;
                }
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        closeAll(() -> client.close());
    }
}
