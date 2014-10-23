package org.cloudbus.mcweb.admissioncontroller;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.cloudbus.mcweb.AdmissionControllerResponse;
import org.cloudbus.mcweb.ServerFarm;
import org.cloudbus.mcweb.UserRequest;

import com.google.common.base.Preconditions;

import static org.cloudbus.mcweb.util.Closeables.*;

/**
 * Represents an entry point in the system. Callers must call one of the
 * {@link configure} methods before using the instance.
 * 
 * @author nikolay.grozev
 */
public class AdmissionController implements AutoCloseable {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(AdmissionController.class.getCanonicalName());
    
    /** Application specific eligibility and performance rules. */
    private IAdmissionControllerRule rules;
    /** The server farm. */
    private ServerFarm serverFarm;
    
    
    /** Singleton instance. */
    private static final AdmissionController instance = new AdmissionController();

    /** Suppress instantiation. */
    private AdmissionController() {
    }
    
    public synchronized static AdmissionController getInstance() {
        return instance;
    }

    /**
     * Configures this admission controller.
     * @param rules - the application specific rules. Must not be null.
     * @param serverFarm - the server farm. Must not be null.
     */
    public synchronized void configure(final IAdmissionControllerRule rules, final ServerFarm serverFarm) {
        Preconditions.checkNotNull(rules);
        Preconditions.checkNotNull(serverFarm);
        
        synchronized (instance) {
            //Release previous resources.
            try {
                close();
            } catch (Exception e) {
                throw new IllegalStateException("Could not close previous Admission Controller", e);
            }
            
            LOG.info("Configure the admission controller.");
            this.rules = rules;
            this.serverFarm = serverFarm;
        }
    }

    /**
     * Returns the responses for the users.
     * @param requests - the end user requests. Must not be null. Elements must not be null.
     * @return the responses for the users.
     */
    public synchronized List<AdmissionControllerResponse> enquire(final List<UserRequest> requests) {
        Preconditions.checkNotNull(requests);
        return requests.stream().map(this::respond).collect(Collectors.toList());
    }

    private AdmissionControllerResponse respond(final UserRequest userRequest) {
        Preconditions.checkNotNull(userRequest);
        boolean eligible = this.rules.isEligible(userRequest.getUserToken());
        double costEstimation = Double.NaN;
        if(eligible) {
            costEstimation = rules.backOff() ? Double.MAX_VALUE : this.serverFarm.costPerUser();
        }
        return new AdmissionControllerResponse(userRequest.getUserToken(), eligible, costEstimation);
    }
    
    @Override
    public synchronized void close() throws Exception {
        LOG.info("Closing the admission controller");
        closeAll(serverFarm, rules);
    }
}
