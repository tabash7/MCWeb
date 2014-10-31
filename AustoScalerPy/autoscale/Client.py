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
    
    def __init__(self, readableName, address, pemFile, runConfig,  userName="ubuntu"):
        super(Client, self).__init__(readableName, address, pemFile, userName)
        self.runConfig = runConfig
        
    def runWorkload(self, loadScale = 50, rampUp = 90, steadyState = 600, rampDown = 10):
        log.info("Prepare run config file:" + self.runConfig)
        inputFile = None
        try:
            inputFile = fileinput.input(self.runConfig, inplace=True)
            for line in inputFile:
                newLine = line
                
                faScaleRegEx = r'(\s*<fa:scale>\s*)\d+(\s*</fa:scale>\s*)'
                rampUpRegEx = r'(\s*<fa:rampUp>\s*)\d+(\s*</fa:rampUp>\s*)'
                steadyStateRegEx  = r'(\s*<fa:steadyState>\s*)\d+(\s*</fa:steadyState>\s*)' 
                rampDownRegEx = r'(\s*<fa:rampDown>\s*)\d+(\s*</fa:rampDown>\s*)' 
                htmlRegExToValues = {faScaleRegEx : loadScale, rampUpRegEx:rampUp, steadyStateRegEx:steadyState, rampDownRegEx:rampDown}
                
                for htmlRegEx in htmlRegExToValues:
                    match = re.match(htmlRegEx, line)
                    if match is not None:
                        newLine = match.group(1) + str(htmlRegExToValues[htmlRegEx]) + match.group(2)
                        break
                
                sys.stdout.write(newLine)
        finally:
            if inputFile is not None: inputFile.close()
            
        # Start the monitoring
        remotePath = os.path.join("/cloudstone/faban", os.path.os.path.basename(self.runConfig))
        log.info("Copying Test/Run Config File: {0} to {1}:{2}".format(self.runConfig, self.readableName, remotePath))
        sftp = self.getSSHClient().open_sftp()
        sftp.put(self.runConfig, remotePath)
        sftp.close()
        
        command = "cd /cloudstone/faban ; ./bin/fabancli submit OlioDriver test " + remotePath
        log.info("Starting workload with loadscale={0} rampUp={1} steadyState={2} rampDown={3}".format(loadScale, rampUp, steadyState, rampDown))
        output = self.execRemoteCommand(command)
        log.info("Workload initiated, output:{0}".format(formatOutput(output)))
        
