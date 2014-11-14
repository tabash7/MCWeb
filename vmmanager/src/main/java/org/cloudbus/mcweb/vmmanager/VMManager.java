package org.cloudbus.mcweb.vmmanager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;

import com.google.common.base.Predicate;

/**
 * A command line utility for managing VM instances. Uses JClouds. Tested with AWS and Openstack on Nectar.
 * 
 * The following commands are supported:
 * <ul>
 * <li>provision - Provisions new VMs</li>
 * <li>terminate - Terminates the specified VMs.</li>
 * </ul>
 * 
 * Command line options are:
 * <ul>
 * <li>providerId - Identifier of Cloud provider - e.g. aws-ec2 or openstack-nova</li>
 * <li>accessKeyId - Access key for the Cloud provider. In Openstack this in the form tenantName:username.</li>
 * <li>secretKey", true, "Secret key/passord"</li>
 * <li>imageOwnerId - Owner of the image. This is important for AWS private images."</li>
 * <li>locationId - Identifier of the location. In AWS this is lower case (e.g. ap-southeast-2)</li>
 * <li>imageId - Identifier of the image. In AWS this is in the form region/id (e.g. ap-southeast-2/ami-XXXX). 
 * In Openstack it is in the form location-id/image-id (e.g. Melbourne/XXXX)</li>
 * <li>hardwareId - The name of the VM type - e.g. m1.small.</li>
 * <li>secGroup - The name of the security group.</li>
 * <li>keyPair - The name of the key-pair.</li>
 * <li>groupName - Readable name for the started instances. Optional parameter. Must be lower case.</li>
 * <li>endPoint - The API endpoint. Typically used in private clouds. Ignored for AWS.</li>
 * <li>numVMs - How many VMs to start.</li>
 * <li>vmsAddresses - addresses of VMs to terminate.</li>
 * </ul>
 * 
 * As a result of the command execution, all affected VMs' addresses (i.e. provisioned or terminated) are
 * printed to the standard output on separate lines.
 * 
 */
public class VMManager {

    private static final String PROVIDER_ID = "providerId";
    private static final String NUM_VMS = "numVMs";
    private static final String END_POINT = "endPoint";
    private static final String GROUP_NAME = "groupName";
    private static final String KEY_PAIR_NAME = "keyPair";
    private static final String SECURITY_GROUP_NAME = "secGroup";
    private static final String HARDWARE_ID = "hardwareId";
    private static final String IMAGE_ID = "imageId";
    private static final String LOCATION_ID = "locationId";
    private static final String IMAGE_OWNER_ID = "imageOwnerId";
    private static final String SECRET_KEY = "secretKey";
    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String VM_ADDRESSES = "vmsAddresses";

    public static void main(String[] args) throws RunNodesException, InterruptedException, ParseException {
        
        Options options = new Options();
        options.addOption(PROVIDER_ID, true, "Identifier of Cloud provider - e.g. aws-ec2 or openstack-nova");
        options.addOption(ACCESS_KEY_ID, true,
                "Access key for the Cloud provider. In Openstack this in the form  tenantName:username ");
        options.addOption(SECRET_KEY, true, "Secret key/passord");
        options.addOption(IMAGE_OWNER_ID, true, "Owner of the image. This is important for AWS private images.");
        options.addOption(LOCATION_ID, true,
                "Identifier of the location. In AWS this is lower case (e.g. ap-southeast-2)");
        options.addOption(IMAGE_ID, true,
                "Idenfier of the image. In AWS this is in the form region/id (e.g. ap-southeast-2/ami-XXX). "
                        + "In Openstack it is in the form location-id/image-id (e.g. Melbourne/XXXX)");
        options.addOption(HARDWARE_ID, true, "The name of the VM type - e.g. m1.small.");
        options.addOption(SECURITY_GROUP_NAME, true, "The name of the security group.");
        options.addOption(KEY_PAIR_NAME, true, "The name of the key-pair.");
        options.addOption(GROUP_NAME, true,
                "Readable name for the started instances. Optional parameter. Must be lower case.");
        options.addOption(END_POINT, true,
                "The API endpoint. Typically used in private clouds. Do not specify it for AWS.");
        options.addOption(NUM_VMS, true, "How many VMs to start.");
        options.addOption(VM_ADDRESSES, true, "Addresses of VMs to destroy.");
         
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);
        
        String command = cmd.getArgs()[0];
        
        String providerId = cmd.getOptionValue(PROVIDER_ID, null);
        String accesskeyid = cmd.getOptionValue(ACCESS_KEY_ID, null);
        String secretkey = cmd.getOptionValue(SECRET_KEY, null);
        String imageOwnerId = cmd.getOptionValue(IMAGE_OWNER_ID, null);
        String locationId = cmd.getOptionValue(LOCATION_ID, null);
        String imageId = cmd.getOptionValue(IMAGE_ID, null);
        String hardwareId = cmd.getOptionValue(HARDWARE_ID, null);
        String securityGroupName = cmd.getOptionValue(SECURITY_GROUP_NAME, null);
        String keyPairName = cmd.getOptionValue(KEY_PAIR_NAME, null);
        String groupName = cmd.getOptionValue(GROUP_NAME, "manager-default");
        String endPoint = cmd.getOptionValue(END_POINT, null);
        int numVMs = Integer.parseInt(cmd.getOptionValue(NUM_VMS, "1"));
        List<String> vmAddresses = Arrays.asList(cmd.getOptionValue(VM_ADDRESSES, "").split("\\s+"));
        
        switch (command) {
            case "provision": {
                final List<String> launchedNodesAddresses = launchInstances(providerId, 
                        accesskeyid, 
                        secretkey, 
                        locationId,
                        imageId, 
                        hardwareId, 
                        securityGroupName, 
                        keyPairName, 
                        groupName, 
                        endPoint, 
                        numVMs,
                        defineImageOnwerIDFilter(providerId, imageOwnerId));
    
                launchedNodesAddresses.stream().forEach(e -> System.out.println(e));
                break;
            }
    
            case "terminate": {
                List<String> terminated = terminateInstances(providerId, 
                        accesskeyid, 
                        secretkey, 
                        endPoint,
                        vmAddresses);
                terminated.stream().forEach(e -> System.out.println(e));
                break;
            }
            
            default:
                throw new IllegalArgumentException("Invalid command " + command);
        }

        System.out.flush();
        System.exit(0);
    }

    private static Properties defineImageOnwerIDFilter(String providerId, String imageOwnerId) {
        Properties imageOwnerIdFilter = new Properties();
        if (providerId.contains("aws-ec2")) {
            imageOwnerIdFilter.setProperty("jclouds.ec2.ami-query", "owner-id=" + imageOwnerId
                    + ";state=available;image-type=machine");
        }
        return imageOwnerIdFilter;
    }

    public static List<String> terminateInstances(
            String providerId,
            String userName,
            String password,
            String endPoint,
            final List<String> addresses) {
        // Get the Compute abstraction for the provider. 
        ComputeService compute = constructCompute(providerId, userName, password, endPoint, new Properties());
        Set<? extends NodeMetadata> destroyed = compute.destroyNodesMatching(new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(NodeMetadata input) {
                for (String address : addresses) {
                    if (input.getPublicAddresses().contains(address.trim())) {
                        return true;
                    }
                }
                return false;
            }
        });
        List<String> result = new ArrayList<>();
        for (NodeMetadata nodeMetadata : destroyed) {
            for (String address : addresses) {
                if (nodeMetadata.getPublicAddresses().contains(address.trim())) {
                    result.add(address.trim());
                }
            }
        }
        
        return result;
    }
    
    public static List<String> launchInstances(
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
        ComputeService compute = constructCompute(providerId, userName, password, endPoint, imageOwnerIdFilter);

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

        return launchedNodesAddresses.stream().map(lst -> lst.get(0)).collect(Collectors.toList());
    }

    private static ComputeService constructCompute(String providerId, String userName, String password,
            String endPoint, Properties imageOwnerIdFilter) {
        ContextBuilder ctxBuilder = ContextBuilder.newBuilder(providerId).
                credentials(userName, password).
                overrides(imageOwnerIdFilter);
        if (endPoint != null){
            ctxBuilder = ctxBuilder.endpoint(endPoint);
        }
        ComputeService compute = ctxBuilder.buildView(ComputeServiceContext.class).getComputeService();
        return compute;
    }

    private static Method keyPairMethod(TemplateOptions options) throws NoSuchMethodException {
        Method keyPairMethod = null;
        for (Method m : options.getClass().getMethods()) {
            if (m.getName().equals("keyPair") || m.getName().equals(KEY_PAIR_NAME) && m.getParameterCount() == 1 || m.getParameterTypes()[0].equals(String.class)) {
                keyPairMethod = m;
                break;
            }
        }
        
        if (keyPairMethod == null) {
            throw new NoSuchMethodException("Could not find key-pair method");
        }
        return keyPairMethod;
    }
}
