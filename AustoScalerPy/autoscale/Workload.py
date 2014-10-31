'''
Created on 11/06/2014

@author: nikolay
'''
from autoscale.BaseAutoscalingClass import BaseAutoscalingClass
import Queue
import time
import threading
import logging

log = logging.getLogger(__name__)


class Workload(BaseAutoscalingClass):
    
    def __init__(self, readableName, client):
        BaseAutoscalingClass.__init__(self, readableName)
        self.client = client
        self.runs = Queue.Queue()
        self.workloadLock = threading.Lock()
        self.currentUsers = 0
        self.thread = None
    
    def addRun(self, loadScale=100, rampUp=90, steadyState=600, rampDown=10):
        log.debug("In Workload \"%s\" added a new runPreTrain: loadScale=%d, rampUp=%d, steadyState=%d, rampDown=%d" % (self.readableName, loadScale, rampUp, steadyState, rampDown))
        self.runs.put((loadScale, rampUp, steadyState, rampDown))
    
    def start(self):
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
        result = 0
        self.workloadLock.acquire()
        result = self.currentUsers
        self.workloadLock.release()
        return result
    
def _startRuns(workload):
    #A wait buffer for starting up the client etc
    waitBuffer = 90
    while not workload.runs.empty():
        run = workload.runs.get()
        workload.client.runWorkload(loadScale=run[0], rampUp=run[1], steadyState=run[2], rampDown=run[3])
        workload._setCurrentNumUsers(run[0])
        wait = waitBuffer + sum(run[1:]);
        if wait > 0:
            time.sleep(wait)

