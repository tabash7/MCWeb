package org.cloudbus.mcweb.rules;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.assertj.core.util.Preconditions;
import org.drools.compiler.compiler.DroolsError;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.compiler.compiler.PackageBuilderErrors;
import org.drools.core.FactHandle;
import org.drools.core.ObjectFilter;
import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.WorkingMemory;
import org.drools.core.event.ObjectInsertedEvent;
import org.drools.core.event.ObjectRetractedEvent;
import org.drools.core.event.ObjectUpdatedEvent;
import org.drools.core.event.WorkingMemoryEventListener;

/**
 * A utility class wrapping the logic of the rule engine.
 * Allows clients to query which users are eligible to get a service within a given data centre.
 * 
 * @author nikolay.grozev
 *
 */
public class RuleEngine {
    
    private static Logger LOG = Logger.getLogger(RuleEngine.class.getCanonicalName());
    
    private static WorkingMemory workingMemory;
    private static final ObjectFilter ADMISSION_DENIED_FILTER = o -> o instanceof AdmissionDenied;
    
    /**
     * Runs the admission control rules, and determines the admission denials - i.e. which users will be denied access. 
     * @param dc - the data centre . Must not be null.
     * @param users - the users. Must not be null. Must not be empty.
     * @return the admission denials.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized static Collection<AdmissionDenied> computeAdmissionDenials(final DataCentre dc, final User ... users){
        Preconditions.checkNotNull(dc);
        Preconditions.checkNotNullOrEmpty(users);
        if(workingMemory == null) {
            RuleBase ruleBase;
            try {
                ruleBase = createRuleBase(RuleEngine.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer1.drl"),
                        RuleEngine.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer2.drl"),
                        RuleEngine.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer3.drl"));
                workingMemory = initializeStatefulWorkingMemory(ruleBase);
            } catch (DroolsParserException | IOException e) {
                LOG.log(Level.SEVERE, "Could not instantiate the rule base", e);
                throw new IllegalStateException(e);
            }
        }
        List<Object> params = new ArrayList<>(Arrays.asList(users));
        params.add(dc);
        return (List)fireAllRulesAndRevert(workingMemory, ADMISSION_DENIED_FILTER, params.toArray());
    }

    /**
     * Returns which users (identified by their ids) will be accepted in the specified data centre.
     * @param dc - the data centre. Must not be null.
     * @param users - the users. Must not be null. Must not contain users with duplicate ids.
     * @return the id of the users. which are *not* eligible for this data centre.
     */
    public static Set<String> determineUsersApplicability(final DataCentre dc, final User ... users){
        Collection<AdmissionDenied> denials = computeAdmissionDenials(dc, users);
        return denials.stream().map(ad -> ad.getUserId()).collect(Collectors.toSet());
    }

    /**
     * Disposes the allocated resources (rules, facts etc.). 
     * Subsequent method calls will start allocating the resources again.
     */
    public static synchronized void dispose() {
        if(workingMemory != null) {
            workingMemory.dispose();
            workingMemory = null;
        }
    }
    
    /**
     * Adds the specified facts to the working memory and executes it. Afterwards all newly created facts are removed
     * and hence the working memory is reverted to its original state. As a result of the rules execution new facts can 
     * be inserted into the working memory. These facts are returned from this method (filtered with the provided filter) 
     * and are removed from the working memory as well.
     * 
     * @param workingMemory - the working memory of the rule engine. Must not be null.
     * @param filter - the filter for the result. Mus not be null.
     * @param facts - the additional facts to add before running the rule engine. Must not be null.
     * @return the newly added facts as a result of the rule base execution. The result is filtered with the
     * specified object filter.
     */
    private static List<Object> fireAllRulesAndRevert (WorkingMemory workingMemory, ObjectFilter filter, Object ... facts) {
        Preconditions.checkNotNull(workingMemory);
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(facts);
        
        // Write results here
        List<Object> result = new ArrayList<>();
        List<FactHandle> handlesToRemove = new ArrayList<>();
        
        // Prepare an event listener to listen for adding new facts
        WorkingMemoryEventListener wmListener = new WorkingMemoryEventListener() {
            @Override
            public void objectUpdated(ObjectUpdatedEvent event) {
                // Do nothing
            }
            
            @Override
            public void objectRetracted(ObjectRetractedEvent event) {
                // Do nothing
            }
            
            @Override
            public void objectInserted(ObjectInsertedEvent event) {
                Object object = event.getObject();
                if(filter.accept(object)) {
                    handlesToRemove.add(event.getFactHandle());
                    result.add(object);
                }
            }
        };
        
        try{
            // Add all facts and run the rule engine
            for (Object fact : facts) {
                LOG.info("Adding fact " + fact.toString() + " to the rule set.");
                handlesToRemove.add(workingMemory.insert(fact));
            }
            
            // Add the listener and run all rules
            workingMemory.addEventListener(wmListener);
            int rulesFired = workingMemory.fireAllRules();
            LOG.info("Fired " + rulesFired + " rules.");
        } finally {
            // Dispose of the listener
            workingMemory.removeEventListener(wmListener);
            // Dispose of the newly inserted objects
            for (FactHandle fh : handlesToRemove) {
                workingMemory.retract(fh);
            }
        }
        return result;
    }
    
    private static RuleBase createRuleBase(InputStream ... ruleStreams) throws IOException, DroolsParserException {
        // Read all rules definitions into a package builder
        PackageBuilder packageBuilder = new PackageBuilder();
        for (InputStream stream : ruleStreams) {
            try(Reader reader = new InputStreamReader(stream)) {
                packageBuilder.addPackageFromDrl(reader);
            }
            assertNoRuleErrors(packageBuilder);
        }
        
        // Add it all to a rule base
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        org.drools.core.rule.Package rulesPackage = packageBuilder.getPackage();
        ruleBase.addPackage(rulesPackage);
        
        return ruleBase;
    }

    private static WorkingMemory initializeStatefulWorkingMemory(RuleBase ruleBase, Object ... objs) {
        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        for (Object object : objs) {
            workingMemory.insert(object);
        }

        return workingMemory;
    }

    private static void assertNoRuleErrors(PackageBuilder packageBuilder) {
        PackageBuilderErrors errors = packageBuilder.getErrors();
        
        if (errors.getErrors().length > 0) {
            StringBuilder errorMessages = new StringBuilder();
            errorMessages.append("Found errors in package builder\n");
            for (int i = 0; i < errors.getErrors().length; i++) {
                DroolsError errorMessage = errors.getErrors()[i];
                errorMessages.append(errorMessage);
                errorMessages.append("\n");
            }
            errorMessages.append("Could not parse knowledge");
            
            throw new IllegalArgumentException(errorMessages.toString());
        }
    }
}
