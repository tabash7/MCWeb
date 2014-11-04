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
    """Performance measurements taken from a VM."""
    
    freqMaxInMhz = 3500
    maxNumCores = 8
    maxCpuCapacity = freqMaxInMhz * maxNumCores
    maxRAMInKb = convertMem(8, fromCode="GB", toCode="KB")
    
    def __init__(self, readableName, vmAddress, serverTime, cpuCapacityMhz, cpuIOWaitPerc, cpuStealPerc, cpuIdlePerc, ramInKb, activeMemInKb, diskUtilPerc, nicUtilPerc, numUsers):
        """
        Constr.
        @param readableName: see superclass.
        @param vmAddress: the address of the VM. Must not be None.
        @param serverTime: the time of the measurement on the VM. Must not be None.
        @param cpuCapacityMhz: The CPU capacity in Mhz. Must not be None. Must be positive.
        @param cpuIOWaitPerc: Percentage of CPU I/O waiting. Must not be None. Must be non-negative.
        @param cpuStealPerc: Percentage of stolen CPU time by the hypervisor. Must not be None. Must be non-negative.
        @param cpuIdlePerc: Percentage of CPU idle time. Must not be None. Must be non-negative.
        @param ramInKb: RAM capacity in Kb. Must not be None. Must be positive.
        @param activeMemInKb: Actively used RAM in Kb. Must not be None. Must be positive and less than the capacity.
        @param diskUtilPerc: Percentage of disk utilisation. Must not be None.
        @param nicUtilPerc: Percentage of NIC utilisation. Must not be None.
        @param numUsers: Number of users served on the system. Must not be None. Must be non-negative. 
        """
        super(VMMeasurement, self).__init__(readableName = readableName)
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
        """
        Returns the normalised CPU capacity of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised CPU capacity.
        """
        return VMMeasurement._assertRatio( (100.0 - self.cpuStealPerc) * self.cpuCapacityMhz / (100 * VMMeasurement.maxCpuCapacity))

    def normaliseRAMCapacity(self):
        """
        Returns the normalised RAM capacity of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised RAM capacity.
        """
        return VMMeasurement._assertRatio( float(self.ramInKb) / VMMeasurement.maxRAMInKb )

    def normaliseCpuUtil(self):
        """
        Returns the normalised CPU utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised CPU utilisation.
        """
        result = (100.0 - self.cpuStealPerc - self.cpuIdlePerc) * self.cpuCapacityMhz / (100 * VMMeasurement.maxCpuCapacity)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedCpuUtil) )

    def cpuUtil(self):
        """
        Returns the CPU utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The CPU utilisation.
        """
        result = (100.0 - self.cpuStealPerc - self.cpuIdlePerc) / (100.0 - self.cpuStealPerc)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevCpuUtil) )

    def normaliseRAMUtil(self):
        """
        Returns the normalised RAM utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised RAM utilisation.
        """
        result = float(self.activeMemInKb) / VMMeasurement.maxRAMInKb
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedRAMUtil) )

    def ramUtil(self):
        """
        Returns the RAM utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The RAM utilisation.
        """
        result = self.activeMemInKb / float(self.ramInKb)
        return VMMeasurement._assertRatio( self._momentum(result, self.prevRamUtil) )
    
    def getAnomaly(self):
        """
        Returns the anomaly score of this VM measurment in the range [0,1]. Considers momentum if set.
        @return: The anomaly score of this measurement.
        """
        return VMMeasurement._assertRatio( self._momentum(self.anomaly, self.prevAnomaly) )

    def normaliseNICUtil(self):
        """
        Returns the normalised NIC utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised NIC utilisation.
        """
        result = self.nicUtilPerc / 100.0
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedNICUtil) )
    
    def normaliseDiskUtil(self):
        """
        Returns the normalised disk utilisation of the VM in the range [0,1]. Considers momentum if set.
        @return: The normalised disk utilisation.
        """
        result = self.diskUtilPerc / 100.0
        return VMMeasurement._assertRatio( self._momentum(result, self.prevNormalisedDiskUtil) )
    
    def numberOfUsers(self):
        """
        Returns the number of users served in the VM.
        @return: The number of users served in the VM.
        """
        result = self.numUsers
        return int(self._momentum(result, self.prevNumUsers))

    def _momentum(self, value, prevValue):
        if self.momentum is not None:
            return (1 - self.momentum) * value + self.momentum * prevValue
        else:
            return value
    
    def considerMomentum(self, prevMeasurement, inputMomentum):
        """
        Modifies this measurment in the light of the previous measurement by using the specified momentum.
        @param prevMeasurement: the previous measurement. If None - no momentum is considered. An instance of VMMeasurement.
        @param inputMomentum: the momentum. If None - no momentum is considered. If not None must in the range [0,1). 
        """
        if prevMeasurement is not None and inputMomentum is not None:
            assert isinstance(prevMeasurement, VMMeasurement), "Prev measurement %s is invalid" % (prevMeasurement)
            assert 0 <= inputMomentum < 1, "Momentum is %s, not in the range [0;1)" % (inputMomentum)
            
            # Do not keep a reference to the previous measure, to avoid a memory leak.
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
        else :
            self.momentum = None
      
    def isValid(self, err=[]):
        """
        Returns if this measurement is valid - i.e. there are no concerns for mismeasurments.
        @param param: an optional output list parameter, where to store all error messages if the measurement is invalid.  
        @return: if this measurement is valid - i.e. there are no concerns for mismeasurments.
        """
        overloadRatio = 0.7
        isOverloaded = (self.cpuUtil() > overloadRatio or self.ramUtil() > overloadRatio or self.normaliseDiskUtil() > overloadRatio or self.normaliseNICUtil() > overloadRatio)
        isUnderloaded = (self.cpuUtil() < 0.1 or self.numberOfUsers() < 25)
        
        #prevUsers = self.prevNumUsers if self.prevNumUsers != 0  else 1 
        #currUsers = self.numUsers if self.numUsers != 0  else 1
        #usersRatio = prevUsers / float(currUsers) #if currUsers > prevUsers else currUsers / float(prevUsers)
        
        #cpuRatio = 0 if self.prevCpuUtil is None or self.cpuUtil() == 0 else self.prevCpuUtil / float(self.cpuUtil())
        #numUsersSwitch = usersRatio < 0.5 or usersRatio > 2 or cpuRatio < 0.5 or cpuRatio > 2
        
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
        """
        Returns the average normalised CPU of the last 3 CPU measurements.
        @return: the average normalised CPU of the last 3 CPU measurements.
        """
        return sum(self.prevNormCpus) / len(self.prevNormCpus) if len(self.prevNormCpus) > 0 else self.normaliseCpuUtil()

    @staticmethod
    def _assertRatio(value):
        assert 0 <= value <= 1, "Value {0} is not in the range [0, 1]" % (value)
        return value
