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
        // String accesskeyid = "AKIAILRWRBMXXTCFZAYA";
        // String secretkey = "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";
        // String imageOwnerId = "575249362288";
        // String locationId = "ap-southeast-2a";
        // String imageId = "ap-southeast-2/ami-111b7e2b";
        // String hardwareId = org.jclouds.ec2.domain.InstanceType.T1_MICRO;
        // String securityGroupName = "CloudStone";
        // String keyPairName = "CloudStone";
        // String groupName = "cloudstone-as"; // Must be lower case

        int i = 0;
        String providerId = args[i++];
        String accesskeyid = args[i++];
        String secretkey = args[i++];
        String imageOwnerId = args[i++];
        String locationId = args[i++];
        String imageId = args[i++];
        String hardwareId = args[i++];
        String securityGroupName = args[i++];
        String keyPairName = args[i++];
        String groupName = args[i++];

        int numVMs = 1;

        Properties imageOwnerIdFilter = new Properties();

        if (providerId.equals("aws-ec2")) {
            imageOwnerIdFilter.setProperty(
            		"jclouds.ec2.ami-query", "owner-id=" + 
            		imageOwnerId +
            		";state=available;image-type=machine");
        }

        final List<List<String>> launchedNodesAddresses = launchInstances(providerId, accesskeyid, secretkey,
                locationId, imageId, hardwareId, securityGroupName, keyPairName, groupName, numVMs, imageOwnerIdFilter);

        System.out.println(launchedNodesAddresses.get(0).get(0));
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
            int numVMs,
            Properties imageOwnerIdFilter) {

    	// Get the Compute abstraction for the provider. 
    	// Override the available VM images
        ComputeService compute = ContextBuilder.
        		newBuilder(providerId).
        		credentials(userName, password).
        		overrides(imageOwnerIdFilter).
        		buildView(ComputeServiceContext.class).getComputeService();

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
            Method keyPairMethod = options.getClass().getMethod("keyPair", String.class);
            keyPairMethod.invoke(options, keyPairName);
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
}
