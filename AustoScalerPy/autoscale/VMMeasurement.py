'''
Created on 04/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
import logging
import datetime
import collections
from autoscale.Util import convertMem

log = logging.getLogger(__name__)

class VMMeasurement(BaseAutoscalingClass):
    
    freqMaxInMhz = 3500
    maxNumCores = 1
    maxCpuCapacity = freqMaxInMhz * maxNumCores
    maxRAMInKb = convertMem(3.75, fromCode="GB", toCode="KB")
    
    def __init__(self, readableName, vmAddress, serverTime, cpuCapacityMhz, cpuIOWaitPerc, cpuStealPerc, cpuIdlePerc, ramInKb, activeMemInKb, diskUtilPerc, nicUtilPerc, numUsers):
        super(VMMeasurement, self).__init__(readableName = readableName)
        ## Validate input
        assert 0 <= cpuCapacityMhz <= VMMeasurement.maxCpuCapacity, "CPU capacity {0} is not in the range[{1}, {2}]".format(cpuCapacityMhz, 0, VMMeasurement.maxCpuCapacity)
        assert 0 <= cpuIOWaitPerc <= 100, "cpuIOWait% {0} is not in the range[{1}, {2}]".format(cpuIOWaitPerc, 0, 100)
        assert 0 <= cpuStealPerc <= 100, "cpuSteal% {0} is not in the range[{1}, {2}]".format(cpuStealPerc, 0, 100)
        assert 0 <= cpuIdlePerc <= 100, "cpuIdle% {0} is not in the range[{1}, {2}]".format(cpuIdlePerc, 0, 100)
        
        assert cpuIOWaitPerc + cpuStealPerc + cpuIdlePerc <= 100, "cpuIOWait% + cpuSteal + cpuIdle% ({0}) is greater than 100".format(cpuIOWaitPerc + cpuStealPerc + cpuIdlePerc)
        
        assert 0 <= ramInKb <= VMMeasurement.maxRAMInKb, "RAM capacity {0} is not in the range[{1}, {2}]".format(ramInKb, 0, VMMeasurement.maxRAMInKb)
        
        if activeMemInKb > ramInKb:
            activeMemInKb = ramInKb
        
        assert 0 <= activeMemInKb <= ramInKb, "Active RAM {0} is not in the range[{1}, {2}]".format(activeMemInKb, 0, ramInKb)
        assert 0 <= diskUtilPerc <= 100, "diskUtil% {0} is not in the range[{1}, {2}]".format(diskUtilPerc, 0, 100)
        assert 0 <= nicUtilPerc <= 100, "nicUtil% {0} is not in the range[{1}, {2}]".format(nicUtilPerc, 0, 100)
        assert 0 <= numUsers, "numUsers {0} is not positive".format(numUsers)

        self.timestamp = datetime.datetime.now()
        self.vmAddress = vmAddress
        self.serverTime = serverTime
        self.cpuCapacityMhz = cpuCapacityMhz
        self.cpuIOWaitPerc = cpuIOWaitPerc
        self.cpuStealPerc = cpuStealPerc
        self.cpuIdlePerc = cpuIdlePerc
        self.ramInKb = ramInKb
        self.activeMemInKb = activeMemInKb
        self.diskUtilPerc = diskUtilPerc
        self.nicUtilPerc = nicUtilPerc
        self.numUsers = numUsers
        
        self.anomaly = 0
        
        self.prevNormalisedCpuUtil = None
        self.prevNormalisedRAMUtil = None
        self.prevNormalisedNICUtil = None
        self.prevNormalisedDiskUtil = None
        self.prevNumUsers = 0
        
        self.prevCpuUtil = None
        self.prevRamUtil = None
        self.prevAnomaly = None
        
        self.prevNormCpus = collections.deque(maxlen=3)
        self.prevUsers = collections.deque(maxlen=3)
        
        self.momentum = None
    
    def normaliseCpuCapacity(self):
        return VMMeasurement._assertRatio( (100.0 - self.cpuStealPerc) * self.cpuCapacityMhz / (100 * VMMeasurement.maxCpuCapacity))

    def normaliseRAMCapacity(self):
        return VMMeasurement._assertRatio( float(self.ramInKb) / VMMeasurement.maxRAMInKb )

    def normaliseCpuUtil(self):
        result = (100.0 - self.cpuStealPerc - self.cpuIdlePerc) * self.cpuCapacityMhz / (100 * VMMeasurement.maxCpuCapacity)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedCpuUtil) )

    def cpuUtil(self):
        result = (100.0 - self.cpuStealPerc - self.cpuIdlePerc) / (100.0 - self.cpuStealPerc)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevCpuUtil) )

    def normaliseRAMUtil(self):
        result = float(self.activeMemInKb) / VMMeasurement.maxRAMInKb
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedRAMUtil) )

    def ramUtil(self):
        result = self.activeMemInKb / float(self.ramInKb)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevRamUtil) )
    
    def getAnomaly(self):
        return VMMeasurement._assertRatio( self._momentum(self.anomaly, self.prevAnomaly) )

    def normaliseNICUtil(self):
        result = self.nicUtilPerc / 100.0
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedNICUtil) )
    
    def normaliseDiskUtil(self):
        result = self.diskUtilPerc / 100.0
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedDiskUtil) )
    
    def numberOfUsers(self):
        result = self.numUsers
        return int(self._momentum(result, self.prevNumUsers))

    def _momentum(self, value, prevValue):
        if self.momentum is not None:
            return (1 - self.momentum) * value + self.momentum * prevValue
        else:
            return value
    
    def considerMomentum(self, prevMeasurement, inputMomentum):
        if prevMeasurement is not None and inputMomentum is not None:
            assert 0 <= inputMomentum < 1, "Momentum is {0}, not in the range [0;1)".format(inputMomentum)
            self.prevNormalisedCpuUtil = prevMeasurement.normaliseCpuUtil()
            self.prevNormalisedRAMUtil = prevMeasurement.normaliseRAMUtil()
            self.prevNormalisedNICUtil = prevMeasurement.normaliseNICUtil()
            self.prevNormalisedDiskUtil = prevMeasurement.normaliseDiskUtil()
            self.prevNumUsers = prevMeasurement.numberOfUsers()
            
            self.prevCpuUtil = prevMeasurement.cpuUtil()
            self.prevRamUtil = prevMeasurement.ramUtil()
            self.prevAnomaly = prevMeasurement.getAnomaly()
        
            self.prevNormCpus.extend(prevMeasurement.prevNormCpus)
            self.prevNormCpus.append(prevMeasurement.normaliseCpuUtil())

            self.prevUsers.extend(prevMeasurement.prevUsers)
            self.prevUsers.append(prevMeasurement.numberOfUsers())
            
            self.momentum = inputMomentum
      
    def isValid(self, err=[]):
        overloadRatio = 0.7
        isOverloaded = (self.cpuUtil() > overloadRatio or self.ramUtil() > overloadRatio or self.normaliseDiskUtil() > overloadRatio or self.normaliseNICUtil() > overloadRatio)
        isUnderloaded = (self.cpuUtil() < 0.1 or self.numberOfUsers() < 25)
        
        prevUsers = self.prevNumUsers if self.prevNumUsers != 0  else 1 
        currUsers = self.numUsers if self.numUsers != 0  else 1
        usersRatio = prevUsers / float(currUsers) #if currUsers > prevUsers else currUsers / float(prevUsers)
        
        cpuRatio = 0 if self.prevCpuUtil is None or self.cpuUtil() == 0 else self.prevCpuUtil / float(self.cpuUtil())
        numUsersSwitch = usersRatio < 0.5 or usersRatio > 2 or cpuRatio < 0.5 or cpuRatio > 2
        
        avgPrevCPU = self.getAvgPrevNormCPU()
        avgNumUsers = sum(self.prevUsers) / len(self.prevUsers) if len(self.prevUsers) > 0 else self.numberOfUsers()
        cpuAvgRatio = self.normaliseCpuUtil() / avgPrevCPU if avgPrevCPU > 0 else 0
        usrsAvgRatio = self.numberOfUsers() / avgNumUsers if avgNumUsers > 0 else 0
        avgSwitch = cpuAvgRatio < 0.5 or cpuAvgRatio > 2 or usrsAvgRatio < 0.5 or usrsAvgRatio > 2

        if isOverloaded: 
            log.info("Invalid because it's overloaded") 
            err.append("overloaded")
        if isUnderloaded: 
            log.info("Invalid because it's underloaded") 
            err.append("underloaded")
        if avgSwitch:
            log.info("Invalid because it's an AVG switch")
            err.append("AVG Switch") 
#         if numUsersSwitch: 
#             log.info("Invalid because it's a switch") 
#             err.append("switch")
            
        return not (isOverloaded or isUnderloaded or avgSwitch)

    def getAvgPrevNormCPU(self):
        return sum(self.prevNormCpus) / len(self.prevNormCpus) if len(self.prevNormCpus) > 0 else self.normaliseCpuUtil()

    @staticmethod
    def _assertRatio(value):
        assert 0 <= value <= 1, "Value {0} is not in the range[{1}, {2}]".format(value, 0, 1)
        return value
