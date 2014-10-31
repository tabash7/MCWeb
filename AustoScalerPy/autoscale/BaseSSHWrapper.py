'''
Created on 05/06/2014

@author: nikolay
'''
import time
import socket
import paramiko
import logging
from paramiko.client import SSHClient
from paramiko.ssh_exception import SSHException
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
from autoscale.Util import formatOutput

log = logging.getLogger(__name__)


class BaseSSHWrapper(BaseAutoscalingClass):
    
    def __init__(self, readableName, address, pemFile, userName="ubuntu", timeout=600):
        super(BaseSSHWrapper, self).__init__(readableName = readableName)
        self.address = address
        self.pemFile = pemFile
        self.userName = userName
        self.timeout = timeout
        
        log.info("Creating entity \"{0}\" at {1}".format(self.readableName, self.address))
        
        self.initSSHClient()
    
    def initSSHClient(self):
        self.client = None
        
        waitPeriod = 10
        attempts = 20
        # Try to connect but not more than 10 times
        for i in range(attempts):
            # If not the first time - sleep a bit. Do not bombard the server with requests
            if i > 0:
                time.sleep(waitPeriod)
            
            # Try establishing ssh client
            try:
                self.client = SSHClient()
                self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
                self.client.connect(self.address, username=self.userName, key_filename=self.pemFile, timeout=self.timeout)    
                break
            except (SSHException, socket.error) as e: 
                self.client = None
                log.error("Could not connect to host {0} err: {1} ".format(self.readableName, str(e)))
        
        # If we could not connect several times - throw an exception
        if self.client == None:
            raise SSHException('Could not connect ' + str(attempts) + ' times in a row')
                
        
    def getSSHClient(self):
        if self.client == None:
            log.warning("SSH connection to {0} will be recreated".format(self.address))
            self.initSSHClient()
        return self.client


    def execRemoteCommand(self, command, asynch=False, recoverFromFailure=True):
        try:
            _, stdout, stderr = self.getSSHClient().exec_command(command)
            output = []
            if not asynch:
                output = stdout.readlines()
                errors = stderr.readlines()
                if errors:
                    log.warning("Error messages encountered when connecting to {0}, messages: {1}".format(self.address, formatOutput(errors)))
            return output
        except (SSHException, socket.error) as e:
            if recoverFromFailure:
                # Mark the connection for re-instantiation and try again
                self.client = None
                return self.execRemoteCommand(command, asynch, False)
            else :
                raise e

    def close(self):
        if self.client != None:
            self.client.close()
            self.client = None
            
    def __del__(self):
        self.close()
