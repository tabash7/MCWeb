package org.cloudbus.mcweb.rules;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<String> rejectedUserIds = admissionDenials.stream().map(ad -> ad.getUserId()).collect(Collectors.toSet());
        assertTrue(rejectedUserIds.contains(euUser1.getUserId()));
        assertTrue(rejectedUserIds.contains(usUser1.getUserId()));
        assertFalse(rejectedUserIds.contains(usUser2.getUserId()));
    }

    @Test
    public void testConsecutiveInvocations() {
        for (int i = 0 ; i < 5 ; i++) {
            testEUUserInUS();
            testEUUserInUSState();
            testEUUserInEU();
            testPCIDoNotMatch();
            testPCIMatch();
            testMutipleUsers();
        }
    }
}
