'''
Created on 10/06/2014

@author: nikolay
'''
from autoscale.Util import execLocal, formatOutput
from autoscale.AppServer import AppServer
import logging
from autoscale.LoadBalancer import LoadBalancer
from autoscale.Client import Client
import os

log = logging.getLogger(__name__)

class VMFactory(object):
    """
    A factory for VMs - i.e. app servers, load balancers etc.
    """
    
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
    
    def createVM(self, readableName, vmType, address = None, htm=None):
        vmAddress = address
        if vmAddress is None:
            log.info("Starting a VM {0} with type {1}".format(readableName, vmType.code))
            vmAddress = self._launchVM(vmType.code)
        
        vm = AppServer(readableName, vmAddress, self.pemFile, vmType, self.monitoringScript, userName=self.userName, htm=htm)
        log.info("Started VM {0} at address {1}".format(readableName, vm.address))
        return vm
    
    def _launchVM(self, hardwareId):
        startVMCommand = "mvn exec:java -Dexec.mainClass=org.cloudbus.provisionvm.JCloudsManager -Dexec.args=\"%s %s %s %s %s %s %s %s %s %s\" -q -f \"%s\"" \
        %(self.providerId, self.accesskeyid, self.secretkey, self.imageOwnerId, self.locationId, self.imageId, hardwareId, self.securityGroupName, \
          self.keyPairName, self.groupName, self.mavenPrjPath)
        
        log.debug("JClouds/Maven command " + startVMCommand)
        
        out = execLocal(startVMCommand)
        
        assert 1 == len(out), "Output should be just an address, but it is:{0}".format(formatOutput(out))
        return out[0]
    
    def createLoadBalancer(self, address):
        return LoadBalancer(readableName="Load Balancer", address=address, pemFile=self.pemFile, userName=self.userName)
    
    def createClient(self, address):
        return Client(readableName="Client", address=address, pemFile=self.pemFile, runConfig=self.runConfig, userName = self.userName)