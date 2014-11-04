'''
Created on 05/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
from autoscale.VMFactory import VMFactory


class AutoScaler(BaseAutoscalingClass):
    
    def __init__(self, readableName, lbAddress, \
                 providerId, accesskeyid, secretkey, \
                 imageOwnerId, locationId, imageId, securityGroupName, keyPairName, groupName, \
                 mavenPrjPath, pemFile, monitoringScript, userName, runConfig):
        """
        Constr.
        @param readableName: see superclass.
        """
        super(AutoScaler, self).__init__(readableName = readableName)
        self.factory = VMFactory(providerId, \
                                  accesskeyid, \
                                  secretkey, \
                                  imageOwnerId, \
                                  locationId, \
                                  imageId, \
                                  securityGroupName, \
                                  keyPairName,\
                                  groupName, \
                                  mavenPrjPath, \
                                  pemFile, \
                                  monitoringScript, \
                                  userName, \
                                  runConfig)
