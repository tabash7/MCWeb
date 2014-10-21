package org.cloudbus.mcweb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ServerFarmTest {

    @Test
    public void test() {
        VMType m1Small = new VMType("m1.smallish", 0.2, 512, 0.7);
        List<Number[]> measurements = Arrays.asList(new Number[] { 0.1, 0.1, 0 }, 
                new Number[] { 0.1, 0.1, 0 },
                new Number[] { 0.1, 0.1, 0 },
                new Number[] { 0.1, 0.1, 0 } );
        VirtualMachine vmNoUsers = new PredefinedVirtualMachine("127.0.0.1", m1Small, measurements, 0);
        
        VMType m1Medium = new VMType("m1.medium", 0.5, 1024, 0.9);
        measurements = Arrays.asList(new Number[] { 0.21, 0.15, 1 }, 
                new Number[] { 0.22, 0.16, 1 },
                new Number[] { 0.23, 0.17, 1 },
                new Number[] { 0.21, 0.13, 1 } );
        VirtualMachine vmOneUser = new PredefinedVirtualMachine("127.0.0.1", m1Small, measurements, 0);
        
        // Farm fetches every 5 secs
        long periodBetweenVMUtilFetching = 5000;
        ServerFarm farm = new ServerFarm(Arrays.asList(vmNoUsers, vmOneUser), periodBetweenVMUtilFetching);
    }

}
