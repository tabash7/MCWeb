'''
Created on 4 Nov 2014

@author: nikolay
'''
import sys
import time
import logging
from autoscale.AppServer import AppServer
from autoscale.Util import convertMem, scriptPath, vmManagerJarPath
from autoscale.VMType import VMType
from autoscale.VMFactory import VMFactory
from autoscale.AWSBillingPolicy import AWSBillingPolicy


##== Configure the level, handler and format for the loggers
rootLogger = logging.getLogger()
rootLogger.setLevel(logging.DEBUG)

ch = logging.StreamHandler(sys.stdout)
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s', datefmt="%H:%M:%S")
ch.setFormatter(formatter)
rootLogger.addHandler(ch)

logging.getLogger('paramiko').setLevel(logging.ERROR)

small = VMType(code="m1.small", declaredCpuCapacity=0.5, declaredRAMCapacityKB=convertMem(0.615, "GB", "KB"), costPerTimeUnit=0.02)

f = VMFactory(providerId="openstack-nova", \
              endPoint="https://keystone.rc.nectar.org.au:5000/v2.0/", \
              accesskeyid="pt-697:n.grozev@student.unimelb.edu.au", \
              secretkey="M2M0ZDZiOTIyNDI4MjQ1", \
              imageOwnerId=None, \
              locationId = "Melbourne", \
              imageId = "Melbourne/b40a036d-3911-4533-84f5-ad565b8376dc", \
              securityGroupName = "AllOpen", \
              keyPairName = "MCCloud", \
              groupName = "test-mccloud", \
              vmManagerJar = vmManagerJarPath(), \
              pemFile = "/home/nikolay/Dropbox/mccloud.pem", \
              monitoringScript = scriptPath("monitor.sh"), \
              userName = "ubuntu", \
              runConfig = scriptPath("monitor.sh"),
              billingPolicy=AWSBillingPolicy())

servers = f.createVMs(readableNames=["TEST"], vmType=small, htm=None, numVMs=1)

for _ in range(20):
    time.sleep(5)
    measurement = servers[0].fetchData(inputMomentum=None)
    if measurement:
        print servers[0]._line(measurement)
