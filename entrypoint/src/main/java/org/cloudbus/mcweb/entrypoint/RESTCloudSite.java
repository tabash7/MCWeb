package org.cloudbus.mcweb.entrypoint;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.cloudbus.mcweb.AdmissionControllerResponse;
import org.cloudbus.mcweb.DataCentre;
import org.cloudbus.mcweb.util.Jsons;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

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
    
    private static final Logger LOG = Logger.getLogger(RESTCloudSite.class.getCanonicalName());
    
    private final Client client;
    private final WebTarget webTarget;
    
    private DataCentre definition;
    
    // For reconnection logic
    private final int reconnectionIntervalInMillis;
    private long connectionLostTimeMillis;
    private boolean connected;
    
    /**
     * Creates cloud sites which the REST service of the admission controllers.
     */
    public static final Function<String[], CloudSite> FACTORY = s -> new RESTCloudSite(s[0], s[1], s[2], Integer.parseInt(s[3]), Integer.parseInt(s[4]));
    
    /**
     * Constructor.
     * @param name - see superclass.
     * @param admissionControllerAddress - see superclass.
     * @param loadBalancerAddress - see superclass.
     * @param connectionTimeoutMillis - time to wait for TCP/HTTP connection establishment. Measured in milliseconds. Must be greater than 1000.
     * @param reconnectionIntervalInMillis - time to wait before attempting to reconnect. Measured in milliseconds. Must be greater than 1000.
     */
    public RESTCloudSite(final String name, final String admissionControllerAddress, final String loadBalancerAddress, 
            int connectionTimeoutMillis, int reconnectionIntervalInMillis) {
        super(name, admissionControllerAddress, loadBalancerAddress);
        Preconditions.checkArgument(connectionTimeoutMillis > 1000);
        Preconditions.checkArgument(reconnectionIntervalInMillis > 1000);

        ClientConfig configuration = new ClientConfig();
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeoutMillis);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, connectionTimeoutMillis);
        client = ClientBuilder.newClient();
        
        webTarget = client.target(getAdmissionControllerAddress()).path(AC_PATH).path(AC_SERVICE_PATH);
        this.reconnectionIntervalInMillis = reconnectionIntervalInMillis;
    }

    @Override
    public void enquire(final List<EPUserRequest> requests) {
        Preconditions.checkNotNull(requests);
        Preconditions.checkArgument(!requests.isEmpty());
        
        // If connected or it's time to try to reconnect...
        if(tryConnection()) {
            try{
            	updateDefinition();
            	
                List<String> userTokens = requests.stream().map(EPUserRequest::getUserToken).collect(Collectors.toList());
                String responseJson = webTarget.queryParam(USER_TOKENS_PARAM, userTokens.toArray()).request(MediaType.APPLICATION_JSON).get(String.class);
                AdmissionControllerResponse[] responses = Jsons.fromJson(responseJson, AdmissionControllerResponse[].class);
                connectionEstablished(true);
            
                LOG.log(Level.INFO, "Cloudsite {0} has been reached. Responses:{1}", new Object[]{toString(), Arrays.toString(responses)});

                // TODO the following could be optimised to avoid nested loops ...
                for (EPUserRequest userRequest : requests) {
                    for (AdmissionControllerResponse response : responses){
                        if (userRequest.getUserToken().equals(response.getUserToken())) {
                            userRequest.addResponseFromCloudSite(new EPAdmissionControllerResponse(response, this));
                            break;
                        }
                    }
                }

            } catch (ProcessingException | WebApplicationException e) {
                // Oops the connection failed ...
            	List<String> userTokens = requests.stream().map(EPUserRequest::getUserToken).collect(Collectors.toList());
            	String call = webTarget.queryParam(USER_TOKENS_PARAM, userTokens.toArray()).getUri().toString(); 
                LOG.log(Level.SEVERE, "Call \"" + call +"\" has failed");
                LOG.log(Level.SEVERE, "Cloudsite \"" + toString() +"\" has become unreachable", e);
                connectionEstablished(false);
            }
        }
    }

    private synchronized void updateDefinition() {
        if (definition == null) {
            WebTarget definitionTarget = client.target(getAdmissionControllerAddress()).path(AC_PATH).path(AC_DC_DEF_PATH);
            String defJson = definitionTarget.request(MediaType.APPLICATION_JSON).get(String.class);
            definition = Jsons.fromJson(defJson, DataCentre.class);
        }
    }

    private synchronized void connectionEstablished(boolean connected) {
        this.connected = connected;
        this.connectionLostTimeMillis = connected ? 0 : System.currentTimeMillis();
    }

    private synchronized boolean tryConnection() {
        return connected || System.currentTimeMillis() - connectionLostTimeMillis >= this.reconnectionIntervalInMillis;
    }
    
    
    /**
     * Returns the data centre definition.
     * @return - the data centre definition.
     */
    public synchronized DataCentre getDefinition() {
		return definition;
	}

	@Override
    public void close() throws Exception {
        closeAll(client::close);
    }
}
