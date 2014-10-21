package org.cloudbus.mcweb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PredefinedVirtualMachineTest {

    @Test
    public void testCostPerUserEstimation() throws Exception {
        double delta = 0.001;
        
        VMType type = new VMType("m1.smallish", 0.2, 512, 0.7);
        List<Number[]> measurements = Arrays.asList(new Number[] { 0.1, 0.1, 0 }, 
                new Number[] { 0.20, 0.15, 1 },
                new Number[] { 0.5, 0.7, 0 },
                new Number[] { 0.30, 0.32, 5 } );
        try (VirtualMachine vm = new PredefinedVirtualMachine("127.0.0.1", type, measurements, 0)) {

            // Measurements: cpu=0.1, ram=0.1, numU=0. Result must be NaN
            vm.fetch();
            assertTrue(Double.isNaN(vm.costPerUser()));
            
            // Measurements: cpu=0.2, ram=0.15, numU=1. Result must be 0.2*cost_per_min
            vm.fetch();
            assertEquals(type.getCostPerMinute() * 0.2, vm.costPerUser(), delta);
            
            // Measurements: cpu=0.5, ram=0.7, numU=0. Result must be NaN
            vm.fetch();
            assertTrue(Double.isNaN(vm.costPerUser()));
            
            // Measurements: cpu=0.2, ram=0.15, numU=1. Result must be cost_per_min * 0.32 / 5
            vm.fetch();
            assertEquals(type.getCostPerMinute() * 0.32 / 5, vm.costPerUser(), delta);
            assertEquals(type.getCostPerMinute() * 0.32 / 5, vm.costPerUser(), delta);
        }
    }

}
