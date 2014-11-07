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
    
    def __init__(self, providerId, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, mavenPrjPath, pemFile, monitoringScript, userName, runConfig):
        """
        Constr.
        @param providerId: the id of the cloud provider. Must not be None. Must be valid as to JClouds.
        @param accesskeyid: the accesskey with the cloud provider. Must not be None. 
        @param secretkey: the secretkey with the cloud provider. Must not be None. 
        @param imageOwnerId: the imageOwnerId with the cloud provider. Must not be None.
        @param locationId: the locationId of the cloud site. Must not be None.
        @param imageId: the image ID of the App server. Must not be None.
        @param securityGroupName: the security group name. Must not be None.
        @param keyPairName: the key-pair name. Must not be None.
        @param groupName: the group name. Must not be None.
        @param mavenPrjPath: the path to the JClouds maven project. Must not be None. Must be valid.
        @param pemFile: The pemFile for SSH authentication.
        @param monitoringScript: the path to the monitoring shell script. Must not be None. Must be valid.
        @param userName: the user name in the VMs. Must not be null.
        @param runConfig: The CloudStone run configuration file. Must not be None. Must be valid.
        """
        
        assert providerId is not None, "Invalid Provider id: %s" % providerId
        assert accesskeyid is not None, "Invalid accesskeyid: %s" % accesskeyid
        assert secretkey is not None, "Invalid secretkey: %s" % secretkey
        assert imageOwnerId is not None, "Invalid imageOwnerId: %s" % imageOwnerId
        assert locationId is not None, "Invalid locationId: %s" % locationId
        assert imageId is not None, "Invalid imageId: %s" % imageId
        assert securityGroupName is not None, "Invalid securityGroupName: %s" % securityGroupName
        assert keyPairName is not None, "Invalid keyPairName: %s" % keyPairName
        assert groupName is not None, "Invalid groupName: %s" % groupName
        assert mavenPrjPath is not None and os.path.isfile(mavenPrjPath), "Invalid mavenPrjPath: %s" % mavenPrjPath
        assert pemFile is not None, "Invalid pemFile: %s" % pemFile
        assert monitoringScript is not None, "Invalid monitoringScript: %s" % monitoringScript
        assert userName is not None, "Invalid userName: %s" % userName
        assert runConfig is not None and os.path.isfile(runConfig), "Invalid runConfig: %s" % runConfig
        
        self.providerId        = providerId
        self.accesskeyid       = accesskeyid
        self.secretkey         = secretkey
        self.imageOwnerId      = imageOwnerId
        self.locationId        = locationId
        self.imageId           = imageId
        self.securityGroupName = securityGroupName
        self.keyPairName       = keyPairName
        self.groupName         = groupName
        self.mavenPrjPath      = mavenPrjPath
        
        self.pemFile            = pemFile
        self.monitoringScript   = monitoringScript
        self.userName           = userName
        self.runConfig          = runConfig
    
    def createVM(self, readableName, vmType, address = None, htm=None, numVMs=1):
        """
        Creates an App Server VM.
        @param readableName: The readable name of the server. Must not be null. 
        @param vmType: The VM type (an instance of VMType). Must not be null.
        @param address: The address of the VM. If None, a new VM will be launched.
        @param htm: The HTM of the VM. If None, a new HTM will be created.
        @return: an App Server VM.
        """
        assert readableName is not None, "The readable name is None"
        assert vmType is not None or not isinstance(vmType, VMType), "Invalid VM type %s" % vmType
        
        vmAddress = address
        if vmAddress is None:
            log.info("Starting a VM %s with type %s", readableName, vmType.code)
            vmAddress = self._launchVM(vmType.code, numVMs=1)
        
        vm = AppServer(readableName, vmAddress, self.pemFile, vmType, self.monitoringScript, userName=self.userName, htm=htm)
        log.info("Started VM %s at address %s", readableName, vm.address)
        return vm
    
    def _launchVM(self, hardwareId, numVMs=1):
        startVMCommand = "mvn exec:java -Dexec.mainClass=org.cloudbus.provisionvm.JCloudsManager -Dexec.args=\"%s %s %s %s %s %s %s %s %s %s\" -q -f \"%s\"" \
        %(self.providerId, self.accesskeyid, self.secretkey, self.imageOwnerId, self.locationId, self.imageId, hardwareId, self.securityGroupName, \
          self.keyPairName, self.groupName, numVMs, self.mavenPrjPath)
        
        log.debug("JClouds/Maven command " + startVMCommand)
        
        out = execLocal(startVMCommand)
        
        assert numVMs == len(out), "Output should be just an address, but it is:{0}".format(formatOutput(out))
        return out
    
    def createLoadBalancer(self, address):
        """
        Creates a load balancer with the specified address.
        @param address: The address of the VM. Must not be null.
        @return: a load balancer with the specified address.
        """
        assert address is not None, "Address is None"
        return LoadBalancer(readableName="Load Balancer", address=address, pemFile=self.pemFile, userName=self.userName)
    
