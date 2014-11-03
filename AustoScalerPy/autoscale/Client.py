'''
Created on 05/06/2014

@author: nikolay
'''
import fileinput
from os.path import os
import re
import sys
from autoscale.BaseSSHWrapper import BaseSSHWrapper
from autoscale.Util import formatOutput
import logging

log = logging.getLogger(__name__)


class Client(BaseSSHWrapper):
    """
    Runs the CloudSim Faban workload. 
    """
    
    def __init__(self, readableName, address, pemFile, runConfig,  userName="ubuntu"):
        """
        Constr.
        @param readableName: see superclass;
        @param address: see superclass.
        @param pemFile: see superclass.  
        @param userName: see superclass.
        @param runConfig: the configuration file for running CloudStone. Must not be None. Must be a valid file. 
        Note! this file may be modified as the client executes. 
        """
        super(Client, self).__init__(readableName, address, pemFile, userName)
        
        assert runConfig is not None, "Run Configuration File is None"
        assert os.path.isfile(runConfig), "File \"%s\" does not exist" % (runConfig) 
        
        self.runConfig = runConfig
        
    def runWorkload(self, loadScale = 50, rampUp = 90, steadyState = 600, rampDown = 10):
        """
        Runs the CloudStone Faban Client asynchrounously. Modifies the Run script.
        @param loadScale: the CloudStone load scale in seconds. Must not be None, 0 or negative.
        @param rampUp: the CloudStone ramp up in seconds. Must not be None, 0 or negative.
        @param steadyState: the CloudStone steady state in seconds. Must not be None, 0 or negative. 
        @param rampDown: the CloudStone ramp down in seconds. Must not be None, 0 or negative.
        """
        assert loadScale is not None and loadScale > 0, "loadScale %s is invalid" % loadScale
        assert rampUp is not None and rampUp > 0, "rampUp %s is invalid" % rampUp
        assert steadyState is not None and steadyState > 0, "steadyState %s is invalid" % steadyState
        assert rampDown is not None and rampDown > 0, "rampDown %s is invalid" % rampDown
        
        log.info("Prepare run config file:%s", self.runConfig)
        
        # Read the file line by line and modify the appropriate sections
        inputFile = None
        try:
            inputFile = fileinput.input(self.runConfig, inplace=True)
            for line in inputFile:
                newLine = line
                
                faScaleRegEx = r'(\s*<fa:scale>\s*)\d+(\s*</fa:scale>\s*)'
                rampUpRegEx = r'(\s*<fa:rampUp>\s*)\d+(\s*</fa:rampUp>\s*)'
                steadyStateRegEx  = r'(\s*<fa:steadyState>\s*)\d+(\s*</fa:steadyState>\s*)' 
                rampDownRegEx = r'(\s*<fa:rampDown>\s*)\d+(\s*</fa:rampDown>\s*)' 
                xmlRegExToValues = {faScaleRegEx : loadScale, rampUpRegEx:rampUp, steadyStateRegEx:steadyState, rampDownRegEx:rampDown}
                
                for htmlRegEx in xmlRegExToValues:
                    match = re.match(htmlRegEx, line)
                    if match is not None:
                        newLine = match.group(1) + str(xmlRegExToValues[htmlRegEx]) + match.group(2)
                        break
                
                sys.stdout.write(newLine)
        finally:
            if inputFile is not None: inputFile.close()
            
        # Start the remote monitoring
        remotePath = os.path.join("/cloudstone/faban", os.path.os.path.basename(self.runConfig))
        log.info("Copying Test/Run Config File: %s to %s:%s", self.runConfig, self.readableName, remotePath)
        sftp = self.getSSHClient().open_sftp()
        sftp.put(self.runConfig, remotePath)
        sftp.close()
        
        # Start the CloudStone client
        command = "cd /cloudstone/faban ; ./bin/fabancli submit OlioDriver test " + remotePath
        log.info("Starting workload with loadscale=%d rampUp=%d steadyState=%d rampDown=%d", loadScale, rampUp, steadyState, rampDown)
        output = self.execRemoteCommand(command)
        log.info("Workload initiated, output:%s", formatOutput(output))
        
