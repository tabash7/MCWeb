'''
Created on 04/06/2014

@author: nikolay
'''
import logging
from os.path import os
from __builtin__ import map

from autoscale.BaseSSHWrapper import BaseSSHWrapper
from autoscale.HTMWrapper import HTMWrapper
from autoscale.Util import extractVal, filterEmptyStrings, formatCurrTime, convertMem
from autoscale.VMMeasurement import VMMeasurement
from autoscale.VMType import VMType


log = logging.getLogger(__name__)


class AppServer(BaseSSHWrapper):
    """An application server in a server farm."""
    
    def __init__(self, readableName, address, pemFile, vmType, monitoringScript, password = None,  userName="ubuntu", htm=None):
        """
        Constr.
        @param readableName: see superclass.
        @param address: see superclass.
        @param pemFile: see superclass.
        @param vmType: The type of the VM. Must not be None. Must be an instance of VMType.
        @param monitoringScript: the location of the monitoring script. Must not be None. Must be valid.
        @param userName: see superclass.
        @param htm: The HTM to use, or None if a new HTM should be created.
        """
        super(AppServer, self).__init__(readableName = readableName, address = address, pemFile = pemFile, userName = userName, password = password)
        
        assert htm is None or isinstance(htm, HTMWrapper), "Invalid HTM type %s" % type(htm)
        assert monitoringScript is not None and os.path.isfile(monitoringScript), "Invalid monitoring script %s" % (monitoringScript)
        assert isinstance(vmType, VMType), "Invalid VM type %s" % (vmType)
        
        self.monitoringScript = monitoringScript
        self.vmType = vmType
        self.htm = htm if htm is not None else HTMWrapper()
        self.lastMeasurement = None
                
        # Start the monitoring
        remotePath = os.path.join("/home", self.userName, os.path.os.path.basename(self.monitoringScript))
        log.info("Copying Monitoring File: %s to %s:%s", self.monitoringScript, self.readableName, remotePath)
        sftp = self.getSSHClient().open_sftp()
        sftp.put(self.monitoringScript, remotePath)
        sftp.close()
        
        log.info("Starting the monitoring script on: %s" % (self.readableName))
        self.execRemoteCommand(command="bash {0} &> /dev/null".format(remotePath), asynch=True)
        
        self.statFile=os.path.expanduser("~/%s-%s.txt" % (readableName, formatCurrTime("%d-%m-%Y-%H:%M:%S")) ) 
        log.info("Record measurements in: %s" %(self.statFile))
        self._header()
        self._numMeas = 0

    def fetchRawData(self):
        """
        Fetch a line of raw data from the monitoring script output.
        @return: a line of raw data from the monitoring script.
        """
        output = self.execRemoteCommand(command = "tail -n 1 ~/monitoring-data.csv")
        assert len(output) < 2, "More than one line was fetched from tail -n 1:"
        return output[0].strip() if output else ""  
            
    @staticmethod
    def _isHeader(data):
        return "cpuIOWaitPerc" in data or "activeMem" in data

    def fetchData(self, inputMomentum, injectVariance = False):
        """
        Fetches measurement data from the server in the form of a VMMeasurement instance.
        @param inputMomentum: The measurement momentum. If None - no momentum will be considered.
        @param injectVariance: Whether to "inject" a change in the workload pattern. Used for experiment purposes.  
        @return: measurement data from the server in the form of a VMMeasurement instance. May return None if no data is available.
        """
        data = filterEmptyStrings( map(extractVal, self.fetchRawData().split(";")) )
        if(data and not AppServer._isHeader(data) and len(data) == 10):
            assert len(data) == 10, "Measurement data %s does not have proper size" % (data)
            log.debug("Measurement %s: %s", self.readableName, data)
            
            varianceActiveMem = data[6] + convertMem(1, fromCode="GB", toCode="KB") + data[9] * convertMem(1, fromCode="MB", toCode="KB")
            varianceIdlePerc = data[4] * 0.9
            
            m = VMMeasurement(readableName = self._measName(),
                             vmAddress=self.address,
                             serverTime = data[0],
                             cpuCapacityMhz = data[1], 
                             cpuIOWaitPerc = data[2], 
                             cpuStealPerc = data[3], 
                             cpuIdlePerc = data[4] if not injectVariance else varianceIdlePerc, 
                             ramInKb = data[5], 
                             activeMemInKb = data[6] if not injectVariance else varianceActiveMem, 
                             diskUtilPerc = data[7], 
                             nicUtilPerc = data[8], 
                             numUsers = data[9])
            
            m.considerMomentum(self.lastMeasurement, inputMomentum)
            self.lastMeasurement = m
            self.vmType.addMeasurement(m)
            self.htm.train(m)
            
            self._line(m)
            return m
        else:
            return None
        
    def _measName(self):
        return "{0}: {1}".format(self.readableName, formatCurrTime(fmt='%d-%m-%Y %H:%M:%S'))
    
    def _header(self, overwrite=True):
        with open(self.statFile, "a+" if not overwrite else "w") as f:
            txt = "%-10s; %-12s; %-8s; %-7s; %-10s; %-10s; %-10s; %-10s; %-10s; %-12s; %-12s; %-12s; %-8s; %-10s" \
            % ("Time", "VMType", "Name", "Users", "Anomaly", "CPU", "RAM", "NormCPU", "NormRAM", "PrevNormCPU", "CPUCapacity", "RAMCapacity", "Valid", "Comment")
            if not overwrite:
                f.write("\n")
            f.write(txt+"\n")
    
    def _line(self, m):
        self._numMeas = self._numMeas + 1
        if self._numMeas % 50 == 0:
            self._header(overwrite=False)
            
        err = []
        valid = m.isValid(err)
        txt = "%-10s; %-12s; %-8s; %-7d; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-12.5f; %-12.5f; %-12.5f; %-8s; %-10s" \
            % (formatCurrTime(fmt="%H:%M:%S"), self.vmType.readableName, self.readableName, m.numberOfUsers(),
               m.getAnomaly(), m.cpuUtil(), m.ramUtil(), m.normaliseCpuUtil(), m.normaliseRAMUtil(),
               m.getAvgPrevNormCPU(),
               m.normaliseCpuCapacity(), m.normaliseRAMCapacity(), str(valid), str(err) )
        with open(self.statFile, "a+") as f:
            f.write(txt+"\n")
        
        return txt
            