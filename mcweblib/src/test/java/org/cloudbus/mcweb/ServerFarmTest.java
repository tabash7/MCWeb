package org.cloudbus.mcweb;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.cloudbus.mcweb.util.Tests.*;

public class ServerFarmTest {

    private static final double delta = 0.001;

    private VMType m1Small;
    private VMType m1Medium;
    private VMType m1Large;
    private VirtualMachine vm1;
    private VirtualMachine vm2;
    private VirtualMachine vm3;
    private VirtualMachine vm4;
    private ServerFarm farm;
    private Random rand;
    
    @Before
    public void setUp(){
        rand = new Random(123l);
        
        // ============= Define 3 VM types - small and medium and large =============
        m1Small = new VMType("m1.small", 0.2, 512, 0.7);
        m1Medium = new VMType("m1.medium", 0.5, 1024, 0.9);
        m1Large = new VMType("m1.large", 0.9, 2048, 0.95);

        // ============= Define 4 VMs with small response delays =============
        // vm1 has no user at all times
        List<Number[]> measurements = null;
        measurements = Arrays.asList(new Number[] { 0.1, 0.1, 0 },
                                     new Number[] { 0.1, 0.1, 0 },
                                     new Number[] { 0.1, 0.1, 0 },
                                     new Number[] { 0.1, 0.1, 0 });
        vm1 = new PredefinedVirtualMachine("127.0.0.1", m1Small, measurements, 10);

        // vm2 has 1 user except for the second fetching
        measurements = Arrays.asList(new Number[] { 0.22, 0.16, 0 },
                                     new Number[] { 0.21, 0.15, 1 },
                                     new Number[] { 0.23, 0.17, 1 },
                                     new Number[] { 0.21, 0.13, 0 });
        vm2 = new PredefinedVirtualMachine("127.0.0.2", m1Medium, measurements, 20);

        // vm3 has different num of users at all times
        measurements = Arrays.asList(new Number[] { 0.15, 0.10, 0 },
                                     new Number[] { 0.21, 0.25, 1 },
                                     new Number[] { 0.53, 0.54, 3 },
                                     new Number[] { 0.90, 0.75, 0 });
        vm3 = new PredefinedVirtualMachine("127.0.0.3", m1Medium, measurements, 30);

        // vm4 has different num of users at all times
        measurements = Arrays.asList(new Number[] { 0.15, 0.10, 0 },
                                     new Number[] { 0.31, 0.15, 1 },
                                     new Number[] { 0.53, 0.27, 3 },
                                     new Number[] { 0.90, 0.75, 0 });
        vm4 = new PredefinedVirtualMachine("127.0.0.4", m1Large, measurements, 40);
        
        // ============= Define the server farm =============
        // Farm fetches every 5 secs
        farm = new ServerFarm(Arrays.asList(vm1, vm2, vm3, vm4), 5000);
    }
    
    @After
    public void tearDown() throws Exception{
        farm.close();
    }
    
    @Test
    public strictfp void testCostEstimation() throws Throwable {
        List<Thread> userThreads = new ArrayList<>();
        AggregatedUncaghtExceptionHandler errHandler = new AggregatedUncaghtExceptionHandler();

        // ============= Run 10 requests in separate threads =============
        for (int i = 0; i < 10; i++) {
            Thread userThread = new Thread(() -> validateServerFarmState());
            userThread.setUncaughtExceptionHandler(errHandler);
            userThread.start();
            userThreads.add(userThread);
        }

        // ============= Wait for the threads to finish =============
        for (Thread userThread : userThreads) {
            userThread.join();
        }
        errHandler.throwFirst();
    }

    private strictfp void validateServerFarmState() {
        long fetchPeriod = farm.getPeriodBetweenVMUtilFetching();
        
        // After about 2 seconds.
        sleep((long) (rand.nextDouble() * 2000));
        double costPerUser = farm.costPerUser();

        assertTrue(Double.isNaN(vm1.costPerUser()));
        assertTrue(Double.isNaN(vm2.costPerUser()));
        assertTrue(Double.isNaN(vm3.costPerUser()));
        assertTrue(Double.isNaN(vm4.costPerUser()));
        assertEquals(0.0, costPerUser, delta);

        // After about 7 seconds - all VMs have no users
        sleep(fetchPeriod);
        double vm2ExpectedCost = m1Medium.getCostPerMinute() * 0.21;
        double vm3ExpectedCost = m1Medium.getCostPerMinute() * 0.25;
        double vm4ExpectedCost = m1Large.getCostPerMinute() * 0.31;
        costPerUser = farm.costPerUser();

        assertTrue(Double.isNaN(vm1.costPerUser()));
        assertEquals(vm2ExpectedCost, vm2.costPerUser(), delta);
        assertEquals(vm3ExpectedCost, vm3.costPerUser(), delta);
        assertEquals(vm4ExpectedCost, vm4.costPerUser(), delta);

        assertEquals((vm2ExpectedCost + vm3ExpectedCost + vm4ExpectedCost) / 3.0, 
                costPerUser, delta);

        // After about 12 seconds.
        sleep(fetchPeriod);
        vm2ExpectedCost = m1Medium.getCostPerMinute() * 0.23;
        vm3ExpectedCost = m1Medium.getCostPerMinute() * 0.54 / 3;
        vm4ExpectedCost = m1Large.getCostPerMinute() * 0.53 / 3;
        costPerUser = farm.costPerUser();

        assertTrue(Double.isNaN(vm1.costPerUser()));
        assertEquals(vm2ExpectedCost, vm2.costPerUser(), delta);
        assertEquals(vm3ExpectedCost, vm3.costPerUser(), delta);
        assertEquals(vm4ExpectedCost, vm4.costPerUser(), delta);
        assertEquals((vm2ExpectedCost + vm3ExpectedCost + vm4ExpectedCost) / 3.0, 
                costPerUser, delta);

        // After about 17 seconds - the last costs must be accurate
        sleep(fetchPeriod);
        double prevCostPerUser = costPerUser;
        costPerUser = farm.costPerUser();

        assertTrue(Double.isNaN(vm1.costPerUser()));
        assertEquals(vm2ExpectedCost, vm2.costPerUser(), delta);
        assertEquals(vm3ExpectedCost, vm3.costPerUser(), delta);
        assertEquals(vm4ExpectedCost, vm4.costPerUser(), delta);
        assertEquals(prevCostPerUser, costPerUser, delta);
        
        // After about 22 seconds - the last costs must be accurate
        sleep(fetchPeriod);
        prevCostPerUser = costPerUser;
        costPerUser = farm.costPerUser();

        assertTrue(Double.isNaN(vm1.costPerUser()));
        assertEquals(vm2ExpectedCost, vm2.costPerUser(), delta);
        assertEquals(vm3ExpectedCost, vm3.costPerUser(), delta);
        assertEquals(vm4ExpectedCost, vm4.costPerUser(), delta);
        assertEquals(prevCostPerUser, costPerUser, delta);
    }
    
}
