package org.cloudbus.mcweb.rules;


import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cloudbus.mcweb.rules.relations.ContainsJurisdiction;
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

public class RuleEngineWrapper {
    
    public static void main(String[] args) throws DroolsParserException, IOException {
     
        //RuleBase ruleBase = createRuleBase(RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/Rules1.drl"));
        RuleBase ruleBase = createRuleBase(RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer1.drl"),
                RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer2.drl"),
                RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/RulesLayer3.drl"));
        WorkingMemory workingMemory = initializeStatefulWorkingMemory(ruleBase, new Message("Hello"));
        
        
        System.out.printf("%n%n<-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -->%n%n");
        List<Object> contains = fireAllRules(workingMemory, o -> o instanceof ContainsJurisdiction);
        contains.stream().forEach(e -> System.out.println(e));

        System.out.printf("%n%n<-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -->%n%n");
        FactHandle euzKMY = workingMemory.insert(new ContainsJurisdiction("EUZ", "KMYanko2"));
        contains = fireAllRules(workingMemory, o -> o instanceof ContainsJurisdiction);
        contains.stream().forEach(e -> System.out.println(e));
        
        System.out.printf("%n%n<-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -->%n%n");
        workingMemory.retract(euzKMY);
        contains = fireAllRules(workingMemory, o -> o instanceof ContainsJurisdiction);
        contains.stream().forEach(e -> System.out.println(e));

        System.out.printf("%n%n<-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -->%n%n");
    }
    
    private static List<Object> fireAllRules (WorkingMemory workingMemory, ObjectFilter filter) {
        // Write results here
        List<Object> result = new ArrayList<>();
        
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
                Object added = event.getObject();
                if(filter.accept(added)) {
                    result.add(added);
                }
            }
        };
        
        try{
            // Add the listener and run all rules
            workingMemory.addEventListener(wmListener);
            workingMemory.fireAllRules();
        } finally {
            // Dispose of the listener
            workingMemory.removeEventListener(wmListener);
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
        assertThat(objs.length).isGreaterThanOrEqualTo(1);
        
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
