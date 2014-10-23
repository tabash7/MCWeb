package org.cloudbus.mcweb;

import static org.cloudbus.mcweb.util.Configs.streamFrom;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.cloudbus.mcweb.util.Configs;
import org.junit.Test;

import com.google.common.base.Preconditions;

public class ConfigUtilTest {
    
    @Test(expected = IllegalStateException.class)  
    public void testParseDuplicateVMTypes() {  
        InputStream is = streamFrom("identifier;      costPerMinute;     ramInMegabytes,        noramalisedCPUCapacity\n"
                + "m1.small;    0.1;    512;    0.2\n"
                + "m1.medium;   0.11;    512.1;    0.21\n"
                + "m1.small;    0.1;    512;    0.2\n"
                + "m1.large;    0.1;    512;    0.2\n");
        Configs.parseVMTypes(is);
    }
    
    @Test
    public void testParseVMTypes() throws IOException {
        
        // Parse 1 VM type
        InputStream is = streamFrom("identifier;      costPerMinute;     ramInMegabytes,        noramalisedCPUCapacity\n"
                + "m1.small;    0.1;    512;    0.2\n");
        List<VMType> vmTypes = Configs.parseVMTypes(is);
        assertEquals(0, is.available());
        assertEquals(1, vmTypes.size());
        assertEquals("m1.small", vmTypes.get(0).getIdentifier());
        assertEquals(0.1, vmTypes.get(0).getCostPerMinute(), 0.01);
        assertEquals(512d, vmTypes.get(0).getRamInMegabytes(), 0.01);
        assertEquals(0.2, vmTypes.get(0).getNormalisedCPUCapacity(), 0.01);

        // Parse 2 VM types
        is = streamFrom("identifier;      costPerMinute;     ramInMegabytes,        noramalisedCPUCapacity\n"
                + "m1.small;    0.1;    512;    0.2\n"
                + "m1.medium;   0.11;    512.1;    0.21");
        vmTypes = Configs.parseVMTypes(is);
        assertEquals(0, is.available());
        assertEquals(2, vmTypes.size());
        assertEquals("m1.small", vmTypes.get(0).getIdentifier());
        assertEquals(0.1, vmTypes.get(0).getCostPerMinute(), 0.01);
        assertEquals(512d, vmTypes.get(0).getRamInMegabytes(), 0.01);
        assertEquals(0.2, vmTypes.get(0).getNormalisedCPUCapacity(), 0.01);
        
        assertEquals("m1.medium", vmTypes.get(1).getIdentifier());
        assertEquals(0.11, vmTypes.get(1).getCostPerMinute(), 0.01);
        assertEquals(512.1d, vmTypes.get(1).getRamInMegabytes(), 0.01);
        assertEquals(0.21, vmTypes.get(1).getNormalisedCPUCapacity(), 0.01);

        // Parse VM types
        is = streamFrom("identifier;      costPerMinute;     ramInMegabytes,        noramalisedCPUCapacity\n");
        vmTypes = Configs.parseVMTypes(is);
        assertEquals(0, is.available());
        assertTrue(vmTypes.isEmpty());
    }

    public static InputStream classLoad(final String resource) {
        Preconditions.checkNotNull(resource);
        return ConfigUtilTest.class.getResourceAsStream(resource);
    }
}
