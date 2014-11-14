'''
Created on 4 Nov 2014

@author: nikolay
'''
from workload.Client import Client
class ClientFactory(object):
    """A factory for Clients."""
    
    def __init__(self, providerId, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, vmManagerJar, pemFile, monitoringScript, userName, runConfig):
        """
        Constr.
        @param providerId: see superclass.
        @param accesskeyid: see superclass. 
        @param secretkey: see superclass. 
        @param imageOwnerId: see superclass.
        @param locationId: see superclass.
        @param imageId: see superclass.
        @param securityGroupName: see superclass.
        @param keyPairName: see superclass.
        @param groupName: see superclass.
        @param vmManagerJar: see superclass.
        @param pemFile: see superclass.
        @param monitoringScript: see superclass.
        @param userName: see superclass.
        @param runConfig: see superclass.
        """
        super(ClientFactory, self).__init__(providerId, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, vmManagerJar, pemFile, monitoringScript, userName, runConfig)
        
    def createClient(self, address):
        """
        Creates a client with the specified address.
        @param address: The address of the VM. Must not be null.
        @return: a client with the specified address.
        """
        assert address is not None, "Address is None"
        return Client(readableName="Client", address=address, pemFile=self.pemFile, runConfig=self.runConfig, userName = self.userName)