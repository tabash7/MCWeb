'''
Created on 7 Nov 2014

@author: nikolay
'''
import logging
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
from autoscale.Util import convertTime
import math

log = logging.getLogger(__name__)

class AWSBillingPolicy(BaseAutoscalingClass):
    """AWS Billing policy."""
    
    def __init__(self):
        """Constr"""
        super(AWSBillingPolicy, self).__init__("AWS Billing Policy")
        
    def nextBillingTime(self, startTime, time):
        """
        Returns the next billing time after the specified moment, given the VM's start time.
        @param startTime: The start time in seconds. May not be None. Must be non-negative.
        @param time: The start time in seconds. May not be None. Must be non-negative. Must be greater than startTime.
        @return: the next billing time after the specified moment, given the VM's start time.  
        """
        assert startTime is not None and startTime >=0, "Invalid start time %s" % (startTime)
        assert time is not None and time >=0, "Invalid time %s" % (time)
        assert time >= startTime, "Time %s is earlier than start time %s" % (time, startTime)
        
        hour = convertTime(1, fromCode="HOUR", toCode="SEC")
        period = time - startTime
        numPeriods = math.floor(period / hour)
        return (numPeriods + 1 ) * hour
        
