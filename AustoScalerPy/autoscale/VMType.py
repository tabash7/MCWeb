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
    
    def __init__(self, code, declaredCpuCapacity, declaredRAMCapacityKB, costPerTimeUnit, numMeasurements = 10):
        super(VMType, self).__init__(code)
        self.code = code
        self.declaredCpuCapacity = declaredCpuCapacity
        self.declaredRAMCapacityKB = declaredRAMCapacityKB
        self.costPerTimeUnit = costPerTimeUnit
        self.measruements = collections.deque([], numMeasurements)
    
    def addMeasurement(self, m):
        if m != None:
            self.measruements.append(m)
    
    def normalisedCPUCapacity(self):
        normCPUCapacities = map(lambda m: m.normaliseCpuCapacity(), self.measruements)
        return float(sum(normCPUCapacities)) / float(len(normCPUCapacities)) if normCPUCapacities else -1

    def normalisedRAMCapacity(self):
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
