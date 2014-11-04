'''
Created on 05/06/2014

@author: nikolay
'''
import time
import socket
import paramiko
import logging
import os
from paramiko.client import SSHClient
from paramiko.ssh_exception import SSHException
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
from autoscale.Util import formatOutput

log = logging.getLogger(__name__)


class BaseSSHWrapper(BaseAutoscalingClass):
    """ A base class, which can connect via SSH to a machine."""
    
    def __init__(self, readableName, address, pemFile=None, password = None, userName="ubuntu", timeout=600):
        """
        Constr.
        @param readableName: A readale description of the machine. Must not be None.
        @param address: The address of the machine. Must not be None.
        @param pemFile: The pem file for SSH authentication.
        @param password: The password for SSH authentication.
        @param userName: The userName for SSH authentication. Must not be None.
        @param timeout: The SSH connection timeout. Must not be None. Must be positive.
        """
        super(BaseSSHWrapper, self).__init__(readableName = readableName)
        
        assert address is not None, "Address is None"
        assert userName is not None, "User name is None"
        assert timeout is not None, "Timeout is None"
        assert timeout > 0, "Timeout is not positive: " + str(timeout)
        assert pemFile is None or os.path.isfile(pemFile), "File \"%s\" does not exist" % (pemFile) 
        
        self.address = address
        self.password = password
        self.pemFile = pemFile
        self.userName = userName
        self.timeout = timeout
        
        log.info("Creating entity \"%s\" at %s", self.readableName, self.address)
        
        self.initSSHClient()
    
    def initSSHClient(self):
        """
        Tries to establish connection with the host.
        @raise SSHException: if the connection could not be established. 
        """
        self.client = None
        
        waitPeriod = 10
        attempts = 20
        # Try to connect but not more than attempts times
        for i in range(attempts):
            # If not the first time - sleep a bit. Do not bombard the server with requests
            if i > 0:
                time.sleep(waitPeriod)
            
            # Try establishing ssh client
            try:
                self.client = SSHClient()
                self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
                if self.pemFile is not None:
                    self.client.connect(self.address, username=self.userName, key_filename=self.pemFile, timeout=self.timeout)
                else :
                    self.client.connect(self.address, username=self.userName, password=self.password, timeout=self.timeout)    
                break
            except (SSHException, socket.error) as e: 
                self.client = None
                log.error("Could not connect to host %s err: %s", self.readableName, str(e))
        
        # If we could not connect several times - throw an exception
        if self.client == None:
            raise SSHException('Could not connect ' + str(attempts) + ' times in a row')
                
        
    def getSSHClient(self):
        """ 
        Retrieves an SSH connection to the machine.
        @return a connection
        @raise SSHException: if the connection could not be established. 
        """
        if self.client == None:
            log.warning("SSH connection to %s will be recreated", self.address)
            self.initSSHClient()
        return self.client

    def execRemoteCommand(self, command, asynch=False, recoverFromFailure=True):
        """ 
        Executes the command on the remove machine.
        @param command: The command to execute on the remote host. Must not be None.
        @param asynch: A boolean flag, whether to run the command asynchrounously or not.
        @param recoverFromFailure: A boolean flag, whether to try to recover if the connection had staled or failed.
        @return the output of the command (a list of text lines), if it was run synchrounously.
        @raise SSHException: if a connection could not be established, or the command gave an error. 
        """
        assert command is not None, "Command is None"
        assert asynch is not None, "Asynch is None"
        assert recoverFromFailure is not None, "recoverFromFailure is None"
        
        try:
            _, stdout, stderr = self.getSSHClient().exec_command(command)
            output = []
            if not asynch:
                output = stdout.readlines()
                errors = stderr.readlines()
                if errors:
                    log.warning("Error messages encountered when connecting to %s, messages: %s", self.address, formatOutput(errors))
            return output
        except (SSHException, socket.error) as e:
            if recoverFromFailure:
                # Mark the connection for re-instantiation and try again
                self.client = None
                return self.execRemoteCommand(command, asynch, False)
            else :
                raise e

    def close(self):
        """ 
        Closes the underlying connection.
        @raise SSHException: if a connection could not be closed. 
        """
        if self.client != None:
            self.client.close()
            self.client = None
            
    def __del__(self):
        self.close()
