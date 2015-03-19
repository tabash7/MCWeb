'''
Created on 05/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
import logging
import threading
import time
from autoscale.Util import currentTimeSecs

log = logging.getLogger(__name__)




class AutoScaler(BaseAutoscalingClass):
    """An autoscaling policy in a Data Centre Controller."""
    
    def __init__(self, readableName, factory, serverFarm, delta, tgrCPU, tgrRAM, n):
        """
        Constr.
        @param readableName: see superclass.
        """
        super(AutoScaler, self).__init__(readableName = readableName)
        self.factory = factory
        
        self.loadBalancer = serverFarm
        
        self.delta = delta
        self.tgrCPU = tgrCPU
        self.tgrRAM = tgrRAM
        self.n = n
        
        self.thread = None
        
    def start(self):
        """Starts the autoscaler."""
        if (self.thread is None) or (not self.thread.isAlive()): 
            log.info("Starting Autoscaler \"%s\" in a separate thread" % (self.readableName))
            self.thread = threading.Thread(target= _start, args=(self,))
            self.thread.setName("Background Thread of " + self.readableName)
            self.thread.setDaemon(True)
            self.thread.start()
            return True
        else:
            log.warning("Autoscaler \"%s\" is already running" % (self.readableName))
            return False
        
    def scale(self):
        nOverloaded = 0
        listFreeVMs = []
        
        # Inspect the status of all AS VMs
        for vm in self.loadBalancer.servers:
            if vm.lastMeasurement:
                vmCPU = vm.lastMeasurement.normaliseCpuUtil()
                vmRam = vm.lastMeasurement.normaliseRAMUtil()
                if vmCPU >= self.tgrCPU and vmRam >= self.tgrRAM:
                    pass
                elif vm.lastMeasurement.numberOfUsers() == 0:
                    listFreeVMs.append(vm)
                    
        nFree = len(listFreeVMs)
        nAS = len(self.loadBalancer.servers)
        allOverloaded = (nOverloaded + nFree == nAS) and nOverloaded > 0
        
        if nFree <= self.n:
            nVmsToStart = 0
            if allOverloaded : 
                nVmsToStart = self.n - nFree + 1
            else:
                nVmsToStart = self.n - nFree
            self.factory...
            #TODO Launch nVmsToStart vms!!!
        else :
            nVmsToStop = 0
            if allOverloaded:
                nVmsToStop = nFree - self.n
            else:
                nVmsToStop = nFree + self.n + 1
            #TODO listFreeVMs.sort(key=lambda x: x.billtime) 
            
            for iteration in range(nVmsToStop):
                billTime = listFreeVMs[iteration].nextBillingTime()
                tCur = currentTimeSecs
                
                if billTime - tCur < self.delta:
                    #TODO terminate listFreeVMs[iteration]
                    pass
                else :
                    break
            
            

def _start(autoscaler):
    
    while True:
        autoscaler.scale()
        time.sleep(autoscaler.delta)
