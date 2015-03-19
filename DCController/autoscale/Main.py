'''
Created on 05/06/2014

@author: nikolay
'''
from __builtin__ import map
import sys
import time
import collections
import logging
from os.path import os
from sys import maxint

from autoscale.FANNWrapper import FANNWrapper
from autoscale.VMType import VMType
from autoscale.Util import convertMem, sigmoid, nextEpoch, getMk, getLrk, formatCurrTime, statHeader, statLine, printTest
from autoscale import VMFactory


log = logging.getLogger(__name__)


##== Get command line arguements
inputUnits = int(sys.argv[1]) if len(sys.argv) > 1 else 1
hiddenUnits = int(sys.argv[2]) if len(sys.argv) > 2 else 250
lr = float(sys.argv[3]) if len(sys.argv) > 3 else 0.001
epochCode = int(sys.argv[4]) if len(sys.argv) > 4 else 1
trainingStatFile=os.path.expanduser("~/RESULTS-NN(%d-%d-%d) LR(%.4f) EpCode(%d)-%s.txt" % (inputUnits, hiddenUnits, 2, lr, epochCode, formatCurrTime(fmt='%d-%measurement-%Y %H:%M:%S'))) 
trainingResFile=os.path.expanduser("~/NN(%d-%d-%d) LR(%.4f) EpCode(%d)-%s.txt" % (inputUnits, hiddenUnits, 2, lr, epochCode, formatCurrTime(fmt='%d-%measurement-%Y %H:%M:%S')))
scalingStatFile=os.path.expanduser("~/Scale-%s.txt" % ( formatCurrTime(fmt='%d-%measurement-%Y %H:%M:%S')))


##== Set up auxiliary result files
printTest(trainingStatFile, None, overwrite=True)
statHeader(trainingResFile, overwrite=True)

##== Configure the level, handler and format for the loggers
rootLogger = logging.getLogger()
rootLogger.setLevel(logging.DEBUG)

ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s', datefmt="%H:%M:%S")
ch.setFormatter(formatter)
rootLogger.addHandler(ch)

logging.getLogger('paramiko').setLevel(logging.ERROR)

##== AWS Access variables - used to create a new VM
providerId = "aws-ec2"
accesskeyid = "AKIAILRWRBMXXTCFZAYA"
secretkey = "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"
imageOwnerId = "575249362288"
locationId = "ap-southeast-2a"
imageId = "ap-southeast-2/ami-59264163"
hardwareId = "t1.micro"
securityGroupName = "CloudStone"
keyPairName = "CloudStone"
groupName = "cloudstone-as"  # Must be lower case
vmManagerJar = "vmmanager.jar"
endPoint = "https://keystone.rc.nectar.org.au:5000/v2.0/"

##== Initialising common variables - addresses, access keys
pemFile = "/home/nikolay/Dropbox/CloudStoneSetupOnUbuntuAdvanced/CloudStone.pem"
monitoringScript = "/home/nikolay/Dropbox/CloudStoneSetupOnUbuntuAdvanced/AutoScaler/monitor.sh"
runConfig = "/home/nikolay/Dropbox/CloudStoneSetupOnUbuntuAdvanced/AutoScaler/run.xml"
userName = "ubuntu"
loadBalancerAddress = "ec2-54-79-203-150.ap-southeast-2.compute.amazonaws.com"
firstAppServerAddress = "ec2-54-253-205-116.ap-southeast-2.compute.amazonaws.com"#"ec2-54-253-144-28.ap-southeast-2.compute.amazonaws.com"
clientAddress = "ec2-54-79-149-247.ap-southeast-2.compute.amazonaws.com"

##== Factory for creating objects that manage VMs, Load Balancer and Clients
factory = VMFactory(providerId, endPoint, accesskeyid, secretkey, imageOwnerId, locationId, imageId, securityGroupName,
                 keyPairName, groupName, vmManagerJar, pemFile, monitoringScript, userName, runConfig, billingPolicy)

##== VM types
t1Micro = VMType(code="t1.micro", declaredCpuCapacity=0.5, declaredRAMCapacityKB=convertMem(0.615, "GB", "KB"), costPerTimeUnit=0.02)
m1Small = VMType(code="m1.small", declaredCpuCapacity=1, declaredRAMCapacityKB=convertMem(1.7, "GB", "KB"), costPerTimeUnit=0.058)
m1Medium = VMType(code="m1.medium", declaredCpuCapacity=2, declaredRAMCapacityKB=convertMem(3.75, "GB", "KB"), costPerTimeUnit=0.117)
m3Medium = VMType(code="m3.medium", declaredCpuCapacity=3, declaredRAMCapacityKB=convertMem(3.75, "GB", "KB"), costPerTimeUnit=0.098)

types = [m1Small, m1Medium, m3Medium]
types.sort(key=lambda t: t.declaredCpuCapacity)

##== Initialise client, first AS servers and Load Balancers
firstAppServer = factory.createVM(readableName="App 1", vmType=m3Medium, address=firstAppServerAddress)
serverFarm = factory.createServerFarm(address=loadBalancerAddress)
serverFarm.addServers(firstAppServer)

##== 
fann = FANNWrapper(topology=(inputUnits, hiddenUnits, 2))

# # ===
#nextAppServer = factory.createVM(readableName="App 2", vmType = t1Micro, address = None)
#log.info("------------->" + nextAppServer.address)
#serverFarm.addServers(nextAppServer)

time.sleep(90)

inputMomentum = 0.0

# Sleep between measurments
defSleepPeriod=5
sleepPeriod=defSleepPeriod

# Used to determine lrk
annomalies = collections.deque(maxlen=10)
rmses = collections.deque(maxlen=10)

# The time in GST seconds of the last autoscaling event and the cool-down period
lastEventTime = int(time.time())
coolDownPeriod = 600

# How many overloaded sequences before autoscaling
conseqTrig = 2

# The last CPU/RAM measurements, used to decide whether to austoscale or not
lastCPUUtils = collections.deque(maxlen=conseqTrig)
lastRAMUtils = collections.deque(maxlen=conseqTrig)

# Min/Max users encountered so far
minUsers, maxUsers = (maxint , 0)

# Counts the number of ANN samples
k=1

initMeasurements = []

iteration = 0
while True:
    iteration = iteration + 1 
    if iteration % 50 == 0:
        printTest(trainingStatFile, fann)
    
    serverMeasurements = []
    for server in serverFarm.servers:
        log.info("Receiving:[" + str(iteration) + "]")
        
        measurement = server.fetchData(inputMomentum = inputMomentum)
        
        ank = measurement.anomaly if measurement is not None and iteration > 105 else 0
        cpu = measurement.normaliseCpuUtil() if measurement is not None else 0
        mem = measurement.normaliseRAMUtil() if measurement is not None else 0
        
        if measurement is not None:
            annomalies.append(ank)
            serverMeasurements.append(measurement)
         
        if measurement is not None and measurement.isValid() :
            minUsers, maxUsers = (min(measurement.numberOfUsers(), minUsers), max(measurement.numberOfUsers(), maxUsers))
            
            if k % 50 == 0:
                statHeader(trainingResFile, overwrite=False);
            
            rmsePre = fann.rmse(measurement.numberOfUsers(), (cpu, mem))
            avgRMSE = rmsePre if len(rmses) == 0 else sum(rmses) / len(rmses)
      
            if rmsePre < 0.01:
                log.info("Predicted well!!! RMSE: %.5f" % (rmsePre))
                time.sleep(sleepPeriod)
                continue

            rmses.append(rmsePre)
            run = fann.run(measurement.numberOfUsers())
            
            annPart = 2**len(annomalies) * reduce(lambda x, y : x * y, map(lambda x : sigmoid(x), annomalies)) 
            rmsePart = max(1.0, rmsePre / avgRMSE)
            
            lrk = getLrk(k=k, lr=lr, rmsePart=rmsePart, annPart=annPart, epochCode = epochCode)
            mk = getMk(k=k, lr=lr, lrk = lrk, epochCode = epochCode)
            
            ek = nextEpoch(lrk=lrk, lr=lr, epochCode=epochCode)
#             sleepPeriod = defSleepPeriod if ek < 2 else defSleepPeriod / 2.0
            
            # Speculated run ...
            fann.config(momentum = mk, learning_rate = lrk)
            rmsePost=fann.train(measurement.numberOfUsers(), (cpu, mem), trainTimes=1, revert = True)
            
            lrkBuff = lrk
            if rmsePost > rmsePre:
                lrk=lr
    
            fann.config(momentum = mk, learning_rate = lrk)
            fann.train(measurement.numberOfUsers(), (cpu, mem), trainTimes=ek, revert = False, maxRMSE=None)
            
            values = (formatCurrTime(), server.readableName, server.vmType.readableName, iteration, k, measurement.numberOfUsers(),
                       cpu, mem, ank, rmsePre, run[0], run[1], ek, mk, lrk, lrkBuff, annPart, rmsePart, annPart*rmsePart, avgRMSE )
            statLine(trainingResFile, values) 
            k = k + 1
        else :
            if measurement is not None:
                txt = "SKIP: %s: [%.3d] %3d, %.5f, %.5f" % (firstAppServer.readableName, iteration, measurement.numberOfUsers(), measurement.cpuUtil(), measurement.ramUtil())
                log.info(txt)
            else: 
                log.info("SKIP: is null:")
    
    cpuUtils = map(lambda sm : sm.cpuUtil() , serverMeasurements)
    ramUtils = map(lambda sm : sm.ramUtil() , serverMeasurements)
    avgCpuUtil = sum(cpuUtils) / len(cpuUtils) if len(cpuUtils) > 0 else 0.0
    avgRamUtil= sum(ramUtils) / len(ramUtils) if len(ramUtils) > 0 else 0.0
    lastCPUUtils.append(avgCpuUtil)
    lastRAMUtils.append(avgRamUtil)
    scaleUpCPU = (len(lastCPUUtils) >= conseqTrig and reduce(lambda x, y: x and y, map(lambda x: x > 0.7, lastCPUUtils)))
    scaleUpRAM = (len(lastRAMUtils) >= conseqTrig and reduce(lambda x, y: x and y, map(lambda x: x > 0.7, lastRAMUtils)))
    
    timeForScaling = ((int(time.time()) - lastEventTime) > coolDownPeriod)
    
    if timeForScaling and (scaleUpCPU or scaleUpRAM):
        log.info("Autoscaling reason: CPU(%s), RAM(%s)" % (str(scaleUpCPU), str(scaleUpRAM)))
        readableName="App "+str(len(serverFarm.servers) + 1)
        
        newtype=m3Medium
        #newtype=VMType.selectVMType(fann, types, scalingStatFile = scalingStatFile, minUsers=minUsers, maxUsers=maxUsers, serverName=readableName)
        
        nextAppServer = factory.createVM(readableName=readableName, vmType = newtype, address = None, htm=firstAppServer.htm.clone())
        log.info("\n\n------------->" + nextAppServer.address)
        serverFarm.addServers(nextAppServer)
        lastEventTime = int(time.time())
                
    time.sleep(sleepPeriod)


printTest(trainingStatFile, fann)
