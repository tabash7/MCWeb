'''
Created on 10/06/2014

@author: nikolay
'''
import os
import logging
from autoscale.Util import execLocal, formatOutput
from autoscale.AppServer import AppServer
from autoscale.LoadBalancer import LoadBalancer
from autoscale.VMType import VMType

log = logging.getLogger(__name__)

class VMFactory(object):
    """A factory for VMs, Clients and load balancer - i.e. app servers, load balancers etc."""
    
    def __init__(self, providerId, endPoint, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, vmManagerJar, pemFile, monitoringScript, userName, runConfig, billingPolicy):
        """
        Constr.
        @param providerId: the id of the cloud provider. Must not be None. Must be valid as to JClouds.
        @param endPoint: The end point of the cloud provider. May be None for public providers like AWS. Must be a valid end point..
        @param accesskeyid: the accesskey with the cloud provider. Must not be None. 
        @param secretkey: the secretkey with the cloud provider. Must not be None. 
        @param imageOwnerId: the imageOwnerId with the cloud provider. Must not be None for AWS.
        @param locationId: the locationId of the cloud site. Must not be None.
        @param imageId: the image ID of the App server. Must not be None.
        @param securityGroupName: the security group name. Must not be None.
        @param keyPairName: the key-pair name. Must not be None.
        @param groupName: the group name. Must not be None.
        @param vmManagerJar: the path to the VM manager jar file. Must not be None. Must be valid.
        @param pemFile: The pemFile for SSH authentication.
        @param monitoringScript: the path to the monitoring shell script. Must not be None. Must be valid.
        @param userName: the user name in the VMs. Must not be null.
        @param runConfig: The CloudStone run configuration file. Must not be None. Must be valid.
        @param billingPolicy: The billing policy for the VMs. Must not be None. 
        """
        
        assert providerId is not None, "Invalid Provider id: %s" % providerId
        assert accesskeyid is not None, "Invalid accesskeyid: %s" % accesskeyid
        assert secretkey is not None, "Invalid secretkey: %s" % secretkey
        assert imageOwnerId is not None or not "aws" in providerId , "Invalid imageOwnerId: %s" % imageOwnerId
        assert locationId is not None, "Invalid locationId: %s" % locationId
        assert imageId is not None, "Invalid imageId: %s" % imageId
        assert securityGroupName is not None, "Invalid securityGroupName: %s" % securityGroupName
        assert keyPairName is not None, "Invalid keyPairName: %s" % keyPairName
        assert groupName is not None, "Invalid groupName: %s" % groupName
        assert vmManagerJar is not None and os.path.isfile(vmManagerJar), "Invalid vmManagerJar: %s" % vmManagerJar
        assert pemFile is not None, "Invalid pemFile: %s" % pemFile
        assert monitoringScript is not None, "Invalid monitoringScript: %s" % monitoringScript
        assert userName is not None, "Invalid userName: %s" % userName
        assert runConfig is not None and os.path.isfile(runConfig), "Invalid runConfig: %s" % runConfig
        assert billingPolicy is not None, "Invalid billingPolicy: %s" % billingPolicy
        
        self.providerId        = providerId
        self.endPoint           = endPoint
        self.accesskeyid       = accesskeyid
        self.secretkey         = secretkey
        self.imageOwnerId      = imageOwnerId
        self.locationId        = locationId
        self.imageId           = imageId
        self.securityGroupName = securityGroupName
        self.keyPairName       = keyPairName
        self.groupName         = groupName
        self.vmManagerJar      = vmManagerJar
        
        self.pemFile            = pemFile
        self.monitoringScript   = monitoringScript
        self.userName           = userName
        self.runConfig          = runConfig
        self.billingPolicy      = billingPolicy

    def loadVm(self, readableName, vmType, address, htm=None):
        """
        Creates an App Server object from already running VM.
        @param readableName: The readable name of the server. Must not be None. 
        @param vmType: The VM type (an instance of VMType). Must not be None.
        @param address: The address of the VM. Must not be None. Must be valid. 
        @param htm: The HTM of the VM. If None, a new HTM will be created.
        @return: an App Server VM.
        """
        assert readableName is not None, "The readable name is None"
        assert vmType is not None or not isinstance(vmType, VMType), "Invalid VM type %s" % vmType
        assert address is not None, "Invalid address %s" % address
        
        vm = AppServer(readableName=readableName, address=address, pemFile=self.pemFile, vmType=vmType, \
                       monitoringScript=self.monitoringScript, userName=self.userName, htm=htm, billingPolicy=self.billingPolicy)
        log.info("Loading VM %s at address %s", readableName, vm.address)
        return vm
    
    def createVMs(self, readableNames, vmType, htm=None, numVMs=1):
        """
        Creates an App Server VM and instantiates the appropriate VM in the cloud.
        @param readableNames: The readable names of the servers. Must not be null. 
        @param vmType: The VM type (an instance of VMType). Must not be null.
        @param htm: The HTM of the VM. If None, a new HTM will be created.
        @return: the App Servers VM.
        """
        assert readableNames is not None, "The readable name is None"
        assert vmType is not None or not isinstance(vmType, VMType), "Invalid VM type %s" % vmType
        
        log.info("Starting VMs %s with type %s", readableNames, vmType.code)
        vmAddresses = self._launchVM(vmType.code, numVMs=numVMs)
        
        vms = []
        for i in range(numVMs):
            vms.append(AppServer(readableName=readableNames[i], address=vmAddresses[i], pemFile=self.pemFile, vmType=vmType, \
                       monitoringScript=self.monitoringScript, userName=self.userName, htm=htm, billingPolicy=self.billingPolicy))
        
        log.info("Started VMs %s at addresses %s", readableNames, vmAddresses)
        return vms
    
    def _launchVM(self, hardwareId, numVMs=1):
        startVMCommand = ("java -jar %s provision " + \
        "-providerId %s " + \
        "-accessKeyId %s " + \
        "-secretKey %s " + \
        "-imageOwnerId %s " + \
        "-locationId %s " + \
        "-imageId %s " + \
        "-hardwareId %s " + \
        "-secGroup %s " + \
        "-keyPair %s " + \
        "-groupName %s " + \
        "-endPoint %s " + \
        "-numVMs %s") \
        %(self.vmManagerJar, self.providerId, self.accesskeyid, self.secretkey, self.imageOwnerId, self.locationId, self.imageId, hardwareId, self.securityGroupName, \
          self.keyPairName, self.groupName, self.endPoint, numVMs)
        
        log.debug("VMManager command " + startVMCommand)
        
        out = execLocal(startVMCommand)
        
        assert numVMs == len(out), "Output should be just %s addresses, but it is:%s" % (numVMs, format(formatOutput(out)))
        return out
    
    def createLoadBalancer(self, address):
        """
        Creates a load balancer with the specified address.
        @param address: The address of the VM. Must not be null.
        @return: a load balancer with the specified address.
        """
        assert address is not None, "Address is None"
        return LoadBalancer(readableName="Load Balancer", address=address, pemFile=self.pemFile, userName=self.userName)
    
