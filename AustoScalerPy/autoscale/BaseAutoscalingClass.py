'''
Created on 04/06/2014

@author: nikolay
'''

import logging

log = logging.getLogger(__name__)

class BaseAutoscalingClass(object):
    
    def  __init__(self, readableName):
        self.readableName = readableName

    def __str__(self):
        return "{0} [{1}]: {2}".format(self.readableName, self.__class__.__name__, str(vars(self)))
