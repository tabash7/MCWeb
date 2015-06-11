package org.cloudbus.mcweb.admissioncontroller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.mcweb.User;

import com.google.common.base.Preconditions;

/**
 * 
 * @author nikolay.grozev
 *
 */
public class TestUserResolver implements IUserResolver {

    private static final String[] allCitizenships = new String[]{"DE", "USA", "AU", "CA"};

    @Override
    public User resolve(String userId) {
        Preconditions.checkNotNull(userId);
        int num = Math.abs(userId.hashCode());
        
        Set<String> citizenships = citizenships(num);
        Set<String> tags = tags(num, citizenships);
        
        User u = new User(userId, citizenships, tags);
        return u;
    }
    
    private static Set<String> citizenships(final int num){
        int idx = num % allCitizenships.length;
        return new HashSet<>(Arrays.asList(allCitizenships[idx]));
    }
    
    private static Set<String> tags(final int num, final Set<String> citizenships){
        Set<String> result = new HashSet<>();
        if(num % 2 == 0){
            result.add("PIC-DSS");
        }
        if(num % 3 == 0 && citizenships.contains("USA")) {
            result.add("US-GOV");
        }
        return result;
    }
}
