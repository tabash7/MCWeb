'''
Created on 11/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
import Queue
import time
import threading
import logging
from workload.Client import Client

log = logging.getLogger(__name__)

class Workload(BaseAutoscalingClass):
    """ Runs the CloudStone Faban workload (i.e. runs) asynchronously. """
    
    def __init__(self, readableName, client):
        """
        Constr.
        @param readableName: see superclass.
        @param client: a Client instance, which is used to run the workloads. Must not be None. 
        """
        BaseAutoscalingClass.__init__(self, readableName)
        
        assert isinstance(client, Client), "Ivalid client: %s" % (client)
        
        self.client = client
        self.runs = Queue.Queue()
        self.workloadLock = threading.Lock()
        self.currentUsers = 0
        self.thread = None
    
    def addRun(self, loadScale=100, rampUp=90, steadyState=600, rampDown=10):
        """
        Schedules a CloudStone run.
        @param loadScale: the CloudStone load scale in seconds. Must not be None, 0 or negative.
        @param rampUp: the CloudStone ramp up in seconds. Must not be None, 0 or negative.
        @param steadyState: the CloudStone steady state in seconds. Must not be None, 0 or negative. 
        @param rampDown: the CloudStone ramp down in seconds. Must not be None, 0 or negative.
        """
        assert loadScale is not None and loadScale > 0, "loadScale %s is invalid" % loadScale
        assert rampUp is not None and rampUp > 0, "rampUp %s is invalid" % rampUp
        assert steadyState is not None and steadyState > 0, "steadyState %s is invalid" % steadyState
        assert rampDown is not None and rampDown > 0, "rampDown %s is invalid" % rampDown
        
        log.debug("In Workload \"%s\" added a new runPreTrain: loadScale=%d, rampUp=%d, steadyState=%d, rampDown=%d" % (self.readableName, loadScale, rampUp, steadyState, rampDown))
        self.runs.put((loadScale, rampUp, steadyState, rampDown))
    
    def start(self):
        """Starts the workload."""
        if (self.thread is None) or (not self.thread.isAlive()): 
            log.info("Starting workload \"%s\" in a separate thread" % (self.readableName))
            self.thread = threading.Thread(target= _startRuns, args=(self,))
            self.thread.setName("Background Thread of " + self.readableName)
            self.thread.setDaemon(True)
            self.thread.start()
            return True
        else:
            log.warning("Workload \"%s\" is already running" % (self.readableName))
            return False
    
    def _setCurrentNumUsers(self, users):
        self.workloadLock.acquire()
        self.currentUsers = users
        self.workloadLock.release()
        
    def getCurrentNumUsers(self):
        """
        Returns the number of users of the current run.
        @return: the number of users of the current run. 
        """
        self.workloadLock.acquire()
        result = self.currentUsers
        self.workloadLock.release()
        return result
    
def _startRuns(workload):
    #A wait buffer for starting up the client 
    waitBuffer = 90
    while not workload.runs.empty():
        run = workload.runs.get()
        workload.client.runWorkload(loadScale=run[0], rampUp=run[1], steadyState=run[2], rampDown=run[3])
        workload._setCurrentNumUsers(run[0])
        wait = waitBuffer + sum(run[1:]);
        if wait > 0:
            time.sleep(wait)

