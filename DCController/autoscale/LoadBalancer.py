'''
Created on 05/06/2014

@author: nikolay
'''
from autoscale.BaseSSHWrapper import BaseSSHWrapper
from autoscale.Util import formatOutput
import logging
from autoscale.AppServer import AppServer

log = logging.getLogger(__name__)

class LoadBalancer(BaseSSHWrapper):
    """Represents a HAProxy load balancer deployed in a VM, with all necessary scripts."""
    
    def __init__(self, readableName, address, pemFile, password=None, userName="ubuntu"):
        """
        Constr.
        @param readableName: see superclass.
        @param address: see superclass.
        @param pemFile: see superclass.
        @param password: see superclass.
        @param userName: see superclass.
        """
        super(LoadBalancer, self).__init__(readableName, address, pemFile, userName)
        self.servers = []
        
    def addServers(self, *servers):
        """
        Adds the servers to the load balancer.
        @param servers: the servers to add. Must not be None. Must be instances of AppServer 
        """
        assert servers is not None, "Servers are None"
        assert reduce(lambda x, y: x and y, map(lambda z:isinstance(z, AppServer), servers)), "Invalid servers %s" % str(servers)
        
        self.servers.extend(servers)
        self._reload()
    
    def remServers(self, *servers):
        """
        Removes the servers from the load balancer.
        @param servers: the servers to remove. Must not be None. Must be instances of AppServer.
        """
        assert servers is not None, "Servers are None"
        assert reduce(lambda x, y: x and y, map(lambda z:isinstance(z, AppServer), servers)), "Invalid servers %s" % str(servers)
        
        for s in servers:
            self.servers.remove(s)
        self._reload()
    
    def _reload(self):
        rrBalanceParam = self._balanceParam()
        command = ". ~/functions.sh; resetLoadBalancer " + rrBalanceParam + "; sudo service haproxy reload"
        log.info("Resetting the load balancer with the following servers and ratios: " + rrBalanceParam)
        output = self.execRemoteCommand(command)
        log.info( "Load balancer output: " + formatOutput(output))
        
    def _balanceParam(self):
        params = []
        for s in self.servers:
            params.append(s.address)
            intCapacity = max(int(round(s.vmType.declaredCpuCapacity)), 1)
            params.append(str(intCapacity))
        return " ".join(params)
