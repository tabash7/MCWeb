package org.cloudbus.mcweb.rules;

import org.cloudbus.mcweb.rules.Message;
import org.drools.compiler.compiler.DroolsError;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.compiler.compiler.PackageBuilderErrors;
import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.WorkingMemory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AppTest {

    @Test
    public void shouldFireHelloWorld() throws IOException, DroolsParserException {
        RuleBase ruleBase = initialiseDrools();
        WorkingMemory workingMemory = initializeMessageObjects(ruleBase);
        int expectedNumberOfRulesFired = 1;

        int actualNumberOfRulesFired = workingMemory.fireAllRules();

        assertThat(actualNumberOfRulesFired, is(expectedNumberOfRulesFired));
    }

    private RuleBase initialiseDrools() throws IOException, DroolsParserException {
        PackageBuilder packageBuilder = readRuleFiles();
        return addRulesToWorkingMemory(packageBuilder);
    }

    private PackageBuilder readRuleFiles() throws DroolsParserException, IOException {
        PackageBuilder packageBuilder = new PackageBuilder();

        String ruleFile = "/org/cloudbus/mcweb/rules/Rules.drl";
        Reader reader = getRuleFileAsReader(ruleFile);
        packageBuilder.addPackageFromDrl(reader);

        assertNoRuleErrors(packageBuilder);

        return packageBuilder;
    }

    private Reader getRuleFileAsReader(String ruleFile) {
        InputStream resourceAsStream = getClass().getResourceAsStream(ruleFile);

        return new InputStreamReader(resourceAsStream);
    }

    private RuleBase addRulesToWorkingMemory(PackageBuilder packageBuilder) {
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        org.drools.core.rule.Package rulesPackage = packageBuilder.getPackage();
        ruleBase.addPackage(rulesPackage);

        return ruleBase;
    }

    private void assertNoRuleErrors(PackageBuilder packageBuilder) {
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

    private WorkingMemory initializeMessageObjects(RuleBase ruleBase) {
        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        createHelloWorld(workingMemory);

        return workingMemory;
    }

    private void createHelloWorld(WorkingMemory workingMemory) {
        Message helloMessage = new Message();
        helloMessage.setType("Hello");
        workingMemory.insert(helloMessage);
    }

}
