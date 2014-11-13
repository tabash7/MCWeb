package org.cloudbus.provisionvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;

/**
 * 
 * 
 */
public class JCloudsManager {

    public static void main(String[] args) throws RunNodesException, InterruptedException {
        // String providerId = "aws-ec2";
        // String accesskeyid = "XXX";
        // String secretkey = "XXX";
        // String imageOwnerId = "XXX";
        // String locationId = "ap-southeast-2a";
        // String imageId = "ap-southeast-2/ami-111b7e2b";
        // String hardwareId = org.jclouds.ec2.domain.InstanceType.T1_MICRO;
        // String securityGroupName = "CloudStone";
        // String keyPairName = "CloudStone";
        // String groupName = "cloudstone-as"; // Must be lower case
        // String endPoint = null;

        // String providerId = "openstack-nova";
        // String accesskeyid = "XXX"; //concat osTenantName and osUsername with a ':';
        // String secretkey = "XXX";
        // String imageOwnerId = "XXX";
        // String locationId = "Melbourne";
        // String imageId = locationId + "/" + "b40a036d-3911-4533-84f5-ad565b8376dc";
        // String hardwareId = "m1.small";
        // String securityGroupName = "AllOpen";
        // String keyPairName = "MCCloud";
        // String groupName = "test-mccloud"; // Must be lower case
        // String endPoint = "https://keystone.rc.nectar.org.au:5000/v2.0/";
        // int numVMs = 2;
        
        int i = 0;
        String providerId = nullF(args[i++]);
        String accesskeyid = nullF(args[i++]);
        String secretkey = nullF(args[i++]);
        String imageOwnerId = nullF(args[i++]);
        String locationId = nullF(args[i++]);
        String imageId = nullF(args[i++]);
        String hardwareId = nullF(args[i++]);
        String securityGroupName = nullF(args[i++]);
        String keyPairName = nullF(args[i++]);
        String groupName = nullF(args[i++]);
        String endPoint = nullF(i < args.length ? args[i++] : null);
        int numVMs = i < args.length ? Integer.parseInt(args[i++]) : 1;

        Properties imageOwnerIdFilter = new Properties();
        if (providerId.equals("aws-ec2")) {
            imageOwnerIdFilter.setProperty(
            		"jclouds.ec2.ami-query", "owner-id=" + 
            		imageOwnerId +
            		";state=available;image-type=machine");
        }

        final List<List<String>> launchedNodesAddresses = launchInstances(providerId, accesskeyid, secretkey,
                locationId, imageId, hardwareId, securityGroupName, keyPairName, groupName, endPoint, numVMs, imageOwnerIdFilter);

        for (List<String> list : launchedNodesAddresses) {
            System.out.println(list.get(0));
        }
        System.out.flush();
        System.exit(0);
    }

    public static List<List<String>> launchInstances(
    		String providerId,
    		String userName,
    		String password,
                String locationId,
                String imageId,
                String hardwareId,
                String securityGroupName,
                String keyPairName,
                String groupName,
                String endPoint,
                int numVMs,
                Properties imageOwnerIdFilter) {

    	// Get the Compute abstraction for the provider. 
    	// Override the available VM images
        ContextBuilder ctxBuilder = ContextBuilder.newBuilder(providerId).
                credentials(userName, password).
                overrides(imageOwnerIdFilter);
        if (endPoint != null){
            ctxBuilder = ctxBuilder.endpoint(endPoint);
        }
        ComputeService compute = ctxBuilder.buildView(ComputeServiceContext.class).getComputeService();

        // In open stack get the hardware id from the name
        if (providerId.toLowerCase().contains("openstack")) {
            for (Hardware hardware : compute.listHardwareProfiles()) {
                if (hardware.getName().equals(hardwareId.toLowerCase().trim())) {
                    hardwareId = hardware.getId();
                    break;
                }
            }
        }
        
        // Create a template for the VM
        Template template = compute.
        		templateBuilder().
        		locationId(locationId).
        		imageId(imageId).
        		hardwareId(hardwareId).build();

        // Specify your own security group
        TemplateOptions options = template.getOptions();
        options.securityGroups(securityGroupName);

        // Specify your own keypair if the current provider allows for this
        try {
            keyPairMethod(options).invoke(options, keyPairName);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalStateException("Provider: " + providerId + " does not support specifying key-pairs.", e);
        }

        final List<List<String>> launchedNodesAddresses = new ArrayList<>();
        try {
            // Launch the instances...
            Set<? extends NodeMetadata> launchedNodesMetadata = compute.createNodesInGroup(groupName, numVMs, template);

            // Collect the addresses ...
            for (NodeMetadata nodeMetadata : launchedNodesMetadata) {
                launchedNodesAddresses.add(new ArrayList<>(nodeMetadata.getPublicAddresses()));
            }
        } catch (RunNodesException e) {
            throw new IllegalStateException("Nodes could not be created.", e);
        }

        return launchedNodesAddresses;
    }

    private static Method keyPairMethod(TemplateOptions options) throws NoSuchMethodException {
        Method keyPairMethod = null;
        for (Method m : options.getClass().getMethods()) {
            if (m.getName().equals("keyPair") || m.getName().equals("keyPairName") && m.getParameterCount() == 1 || m.getParameterTypes()[0].equals(String.class)) {
                keyPairMethod = m;
                break;
            }
        }
        
        if (keyPairMethod == null) {
            throw new NoSuchMethodException("Could not find key-pair method");
        }
        return keyPairMethod;
    }
    
    private static String nullF(String p) {
        if(p == null || p.trim().equalsIgnoreCase("null") || p.trim().equalsIgnoreCase("none")) {
            return null;
        } else {
            return null;
        }
    }
}
