package org.cloudbus.mcweb.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudbus.mcweb.AggregatedUncaghtExceptionHandler;
import org.cloudbus.mcweb.util.Tests;
import org.junit.AfterClass;
import org.junit.Test;

public class RuleEngineTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testEUUserInUS() {
        User euUser = new User("EU-User", new HashSet<String>(Arrays.asList("BG")), (Set<String>) Collections.EMPTY_SET);
        DataCentre usDC = new DataCentre("US", "AWS", (Set<String>) Collections.EMPTY_SET);

        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(usDC, euUser);

        // One user only should be denied
        assertEquals(1, admissionDenials.size());
        assertEquals(euUser.getUserId(), admissionDenials.toArray(new AdmissionDenied[0])[0].getUserId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEUUserInUSState() {
        User euUser = new User("EU-User", new HashSet<String>(Arrays.asList("BG")), (Set<String>) Collections.EMPTY_SET);
        DataCentre usDC = new DataCentre("USA-AZ", "AWS", (Set<String>) Collections.EMPTY_SET);

        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(usDC, euUser);

        // One user only should be denied
        assertEquals(1, admissionDenials.size());
        assertEquals(euUser.getUserId(), admissionDenials.toArray(new AdmissionDenied[0])[0].getUserId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEUUserInEU() {
        User euUser = new User("EU-User", new HashSet<String>(Arrays.asList("BG")), (Set<String>) Collections.EMPTY_SET);
        DataCentre euDC = new DataCentre("GB", "AWS", (Set<String>) Collections.EMPTY_SET);

        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(euDC, euUser);

        // No user should be denied
        assertTrue(admissionDenials.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPCIDoNotMatch() {
        User euUser = new User("EU-User", new HashSet<String>(Arrays.asList("BG")), new HashSet<String>(Arrays.asList("PCI-DSS")));
        DataCentre euDC = new DataCentre("GB", "AWS", (Set<String>) Collections.EMPTY_SET);

        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(euDC, euUser);

        // One user only should be denied
        assertEquals(1, admissionDenials.size());
        assertEquals(euUser.getUserId(), admissionDenials.toArray(new AdmissionDenied[0])[0].getUserId());
    }

    @Test
    public void testPCIMatch() {
        User euUser = new User("EU-User", new HashSet<String>(Arrays.asList("BG")), new HashSet<String>(Arrays.asList("PCI-DSS")));
        DataCentre euDC = new DataCentre("GB", "AWS", new HashSet<String>(Arrays.asList("PCI-DSS")));
        
        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(euDC, euUser);

        // No user should be denied
        assertTrue(admissionDenials.isEmpty());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testMutipleUsers() {
        // Will be rejected for PCI-DSS compliance and location
        User euUser1 = new User("EU-User1", new HashSet<String>(Arrays.asList("BG")), new HashSet<String>(Arrays.asList("PCI-DSS")));

        // Will be rejected for PCI-DSS compliance
        User usUser1 = new User("US-User1", new HashSet<String>(Arrays.asList("USA-AZ")), new HashSet<String>(Arrays.asList("PCI-DSS")));

        // Will be accepted
        User usUser2 = new User("US-User2", new HashSet<String>(Arrays.asList("USA-WA")), (Set<String>) Collections.EMPTY_SET);
        
        DataCentre usDC = new DataCentre("USA", "AWS", (Set<String>) Collections.EMPTY_SET);
        
        Collection<AdmissionDenied> admissionDenials = RuleEngine.computeAdmissionDenials(usDC, euUser1, usUser1, usUser2);
        
        // There should be 3 rejections
        assertEquals(3, admissionDenials.size());
        
        //First two users should be rejected - the last one not
        Set<String> rejectedUserIds = admissionDenials.stream().map(AdmissionDenied::getUserId).collect(Collectors.toSet());
        assertTrue(rejectedUserIds.contains(euUser1.getUserId()));
        assertTrue(rejectedUserIds.contains(usUser1.getUserId()));
        assertFalse(rejectedUserIds.contains(usUser2.getUserId()));
        
        // Call the other method as well
        rejectedUserIds = RuleEngine.determineUsersApplicability(usDC, usUser2, euUser1, usUser1);
        assertTrue(rejectedUserIds.contains(euUser1.getUserId()));
        assertTrue(rejectedUserIds.contains(usUser1.getUserId()));
        assertFalse(rejectedUserIds.contains(usUser2.getUserId()));
    }

    @Test
    public void testConsecutiveInvocations() {
        for (int i = 0 ; i < 10 ; i++) {
            testEUUserInUS();
            testEUUserInUSState();
            testEUUserInEU();
            testPCIDoNotMatch();
            testPCIMatch();
            testMutipleUsers();
            
            // Every now and again dispose the allocated resources
            if(i % 3 == 0) {
                RuleEngine.dispose();
            }
        }
    }

    @Test
    public void testRulesInMultipleThreads() throws Throwable {
        AggregatedUncaghtExceptionHandler errHandler = new AggregatedUncaghtExceptionHandler();
        final Random random = new Random(100);
        List<Thread> threads = new ArrayList<>();
        
        //testEUUserInUS thread
        Thread testEUUserInUS = new Thread(() -> {
            Tests.sleep(random.nextDouble() * 100);
            testEUUserInUS();
            });
        testEUUserInUS.setName("testEUUserInUS");
        testEUUserInUS.setUncaughtExceptionHandler(errHandler);
        threads.add(testEUUserInUS);
        testEUUserInUS.start();
                
        //testEUUserInUSState thread
        Thread testEUUserInUSState = new Thread(() -> {
            Tests.sleep(random.nextDouble() * 100);
            testEUUserInUSState();
        });
        testEUUserInUSState.setName("testEUUserInUS");
        testEUUserInUSState.setUncaughtExceptionHandler(errHandler);
        threads.add(testEUUserInUSState);
        testEUUserInUSState.start();
        
        //testMutipleUsers thread
        Thread testMutipleUsers = new Thread(() -> {
            Tests.sleep(random.nextDouble() * 100);
            testMutipleUsers();
        });
        testMutipleUsers.setName("testEUUserInUS");
        testMutipleUsers.setUncaughtExceptionHandler(errHandler);
        threads.add(testMutipleUsers);
        testMutipleUsers.start();

        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }
        
        // If any thread has failed - raise the first exception
        errHandler.throwFirst();
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        RuleEngine.dispose();
    }
}
