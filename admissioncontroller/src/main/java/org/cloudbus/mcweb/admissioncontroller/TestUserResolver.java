package org.cloudbus.mcweb.admissioncontroller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.mcweb.rules.User;

import com.google.common.base.Preconditions;

/**
 * 
 * @author nikolay.grozev
 *
 */
public class TestUserResolver implements IUserResolver {

    @Override
    public User resolve(String userId) {
        Preconditions.checkNotNull(userId);
        int num = Math.abs(userId.hashCode());
        
        User u = new User(userId, citizenships(num), tags(num));
        return u;
    }
    
    private static Set<String> citizenships(int num){
        String[] allCitizenships = new String[]{"AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB", "GR", "HU", "HR", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PO", "PT", "RO", "SE", "SI", "SK"};
        int idx = num % allCitizenships.length;
        return new HashSet<>(Arrays.asList(allCitizenships[idx]));
    }
    
    private static Set<String> tags(int num){
        String[] allCitizenships = new String[]{"PCI-DSS", "Test"};
        int idx = num % allCitizenships.length;
        return new HashSet<>(Arrays.asList(allCitizenships[idx]));
    }
}
