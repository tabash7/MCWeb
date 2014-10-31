'''
Created on 10/06/2014

@author: nikolay
'''
from autoscale.Util import execLocal, formatOutput
from autoscale.AppServer import AppServer
import logging
from autoscale.LoadBalancer import LoadBalancer
from autoscale.Client import Client

log = logging.getLogger(__name__)

class VMFactory(object):
    
    def __init__(self, providerId, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, mavenPrjPath, pemFile, monitoringScript, userName, runConfig):
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