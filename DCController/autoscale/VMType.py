'''
Created on 03/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
import collections
from __builtin__ import map
import logging
from autoscale.VMMeasurement import VMMeasurement
from sys import maxint
from autoscale.Util import formatCurrTime

log = logging.getLogger(__name__)


class VMType(BaseAutoscalingClass):
    """A Virtual Machine type."""
    
    def __init__(self, code, declaredCpuCapacity, declaredRAMCapacityKB, costPerTimeUnit, numMeasurements = 10):
        """
        Constr.
        @param code: the code of the VM type - e.g. "m1.small". Must not be None.
        @param declaredCpuCapacity: the declared CPU capacity (e.g. ECU). Must not be None. Must be positive.
        @param declaredRAMCapacityKB: the declared RAM capacity in KB. Must not be None. Must be positive.
        @param costPerTimeUnit: the cost per time unit (e.g. per hour or minute). Must not be None. Must be positive.
        @param numMeasurements: how many measurements to keep to determine the capacity. Must not be None. Must be a positive integer
        """
        super(VMType, self).__init__(code)
        
        assert code is not None, "VM type code is None"
        assert declaredCpuCapacity is not None and declaredCpuCapacity > 0, "Invalid declared CPU capacity %s" % (declaredCpuCapacity)
        assert declaredRAMCapacityKB is not None and declaredRAMCapacityKB > 0, "Invalid declared RAM capacity %s" % (declaredRAMCapacityKB)
        assert costPerTimeUnit is not None and costPerTimeUnit > 0, "Invalid cost %s" % (costPerTimeUnit)
        assert numMeasurements is not None and numMeasurements > 0 and isinstance(numMeasurements, int), "Invalid number of measurements %s" % (numMeasurements)
        
        self.code = code
        self.declaredCpuCapacity = declaredCpuCapacity
        self.declaredRAMCapacityKB = declaredRAMCapacityKB
        self.costPerTimeUnit = costPerTimeUnit
        self.measruements = collections.deque([], numMeasurements)
    
    def addMeasurement(self, m):
        """
        Adds a measurement for this VM type. If None - nothing is added.
        @param m: The measurement. If not None, must be an instance of VMMeasurement. 
        """
        if m != None:
            assert isinstance(m, VMMeasurement), "Invalid measurement %s" % (m)
            self.measruements.append(m)
    
    def normalisedCPUCapacity(self):
        """
        Returns the normalised CPU capacity of the VM type - in the range [0,1].
        @return: the normalised CPU capacity of the VM type - in the range [0,1].
        """
        normCPUCapacities = map(lambda m: m.normaliseCpuCapacity(), self.measruements)
        return float(sum(normCPUCapacities)) / float(len(normCPUCapacities)) if normCPUCapacities else -1

    def normalisedRAMCapacity(self):
        """
        Returns the normalised RAM capacity of the VM type - in the range [0,1].
        @return: the normalised RAM capacity of the VM type - in the range [0,1].
        """
        return float(self.declaredRAMCapacityKB) / VMMeasurement.maxRAMInKb
    
    @staticmethod
    def inferNormalisedCPUCapacity(targetVMType, vmTypes):
        cpuCap = targetVMType.normalisedCPUCapacity()
        # If we could not define the CPU capacity based on the measurements
        # then we'll infer it based on the other vmTypes 
        if cpuCap <= 0:
            measuredTypes=filter(lambda t: t.normalisedCPUCapacity() > 0, vmTypes)
            scaledCapacities = map(lambda t: (t.normalisedCPUCapacity() * targetVMType.declaredCpuCapacity ) / t.declaredCpuCapacity , measuredTypes )
            cpuCap = float(sum(scaledCapacities)) / len(scaledCapacities)
        
        return cpuCap
    
    @staticmethod
    def selectVMType(fann, vmTypes, scalingStatFile, minUsers, maxUsers, serverName, delta=5, maxUtil=0.7):
        with open(scalingStatFile, "a+") as f:
            txt = "\n\n=== %s: Selecting VM type for \"%s\", minU(%d) maxU(%d)" % (formatCurrTime(), serverName, minUsers, maxUsers)
            f.write(txt+"\n")
        bestVMt = None
        bestCost = maxint;
        
        # For all vms
        for vmt in vmTypes:
            cpuCapacity = VMType.inferNormalisedCPUCapacity(vmt, vmTypes)
            ramCapacity = vmt.normalisedRAMCapacity()
    
            # Find how many users can it take
            n = minUsers
            uCapacity = 1
            while True:
                cpu = maxint
                ram = maxint
                if n > maxUsers:
                    runMin = fann.run(minUsers)
                    cpuMin = runMin[0]
                    ramMin = runMin[1] 
                    runMax = fann.run(maxUsers)
                    cpuMax = runMax[0]
                    ramMax = runMax[1]
                    cpuPerUser = (cpuMax - cpuMin) / float(maxUsers - minUsers) 
                    ramPerUser = (ramMax - ramMin) / float(maxUsers - minUsers)
                    cpu = cpuMax + (n - maxUsers) * cpuPerUser 
                    ram = ramMax + (n - maxUsers) * ramPerUser
                else :
                    run = fann.run(n)
                    cpu = run[0]
                    ram = run[1]
                
                with open(scalingStatFile, "a+") as f:
                    txt = "%-10s : VM type=%-15s, n=%-4d, CPUpred=%.4f, RAMpred=%.4f, CPUCap=%.4f, RAMCap=%.4f " % \
                    (formatCurrTime(), vmt.readableName, n, cpu, ram, maxUtil*cpuCapacity, maxUtil*ramCapacity )
                    f.write(txt+"\n")
                
                if cpu < maxUtil*cpuCapacity and ram < maxUtil*ramCapacity:
                    uCapacity = n
                else:
                    break
                n = n + delta
    
            # Compute the cost per user        
            uCost = vmt.costPerTimeUnit / uCapacity 
            txt1 ="Definition of VMType \"%s\": CPU(%.4f), RAM(%.4f), Cost (%.4f) " % (vmt.readableName, cpuCapacity, ramCapacity, vmt.costPerTimeUnit)
            txt2 = "Capacity of VMType \"%s\": CPU(%.4f), RAM(%.4f), Users(%d), Cost per user(%.6f)  " % (vmt.readableName, cpu, ram, uCapacity, uCost)
            log.info(txt1)
            log.info(txt2)
            
            with open(scalingStatFile, "a+") as f:
                f.write(txt1+"\n")
                f.write(txt2+"\n=======\n")
            
            
            # Choose the best type
            if uCost < bestCost:
                bestCost = uCost
                bestVMt = vmt
        
        with open(scalingStatFile, "a+") as f:
            f.write("--> Selected VM type:" + bestVMt.readableName +"\n")    
        return bestVMt
