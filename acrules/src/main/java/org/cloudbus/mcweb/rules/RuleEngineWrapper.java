package org.cloudbus.mcweb.rules;


import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

import org.assertj.core.api.Condition;
import org.drools.compiler.compiler.DroolsError;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.compiler.compiler.PackageBuilderErrors;
import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.WorkingMemory;

public class RuleEngineWrapper {
    
    public static void main(String[] args) throws DroolsParserException, IOException {
     
        RuleBase ruleBase = createRuleBase(RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/Rules1.drl"));
        WorkingMemory workingMemory = initializeStatefulWorkingMemory(ruleBase, new Message("Hello"));
        int actualNumberOfRulesFired = workingMemory.fireAllRules();
        
        //assertThat(actualNumberOfRulesFired).isEqualTo(1);
    }
    
    public static void shouldFireHelloWorld() throws IOException, DroolsParserException {
        RuleBase ruleBase = createRuleBase(RuleEngineWrapper.class.getResourceAsStream("/org/cloudbus/mcweb/rules/Rules.drl"));
        WorkingMemory workingMemory = initializeStatefulWorkingMemory(ruleBase, new Message("Hello"));
        int actualNumberOfRulesFired = workingMemory.fireAllRules();
        
        assertThat(actualNumberOfRulesFired).isEqualTo(1);
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
