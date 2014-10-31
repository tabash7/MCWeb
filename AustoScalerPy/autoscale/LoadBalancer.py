'''
Created on 05/06/2014

@author: nikolay
'''
from autoscale.BaseSSHWrapper import BaseSSHWrapper
from autoscale.Util import formatOutput
import logging

log = logging.getLogger(__name__)

class LoadBalancer(BaseSSHWrapper):
    
    def __init__(self, readableName, address, pemFile, userName="ubuntu"):
        super(LoadBalancer, self).__init__(readableName, address, pemFile, userName)
        self.servers = []
        
    def addServers(self, *servers):
        self.servers.extend(servers)
        self._reload()
    
    def remServers(self, *servers):
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
