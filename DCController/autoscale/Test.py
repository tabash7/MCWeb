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
from math import *



fann = FANNWrapper(topology=(1, 250, 2));

for k in range(1, 1000):
    for i in range(k, 10000, k):
        expOutput = [log(i * 10), log(i * 10)]  
        rmse = fann.train(n=i, expOutput=expOutput, trainTimes = 1, revert=False, maxRMSE=0.05)
        print "Training with i = %d, RMSE: %.4f, output=[%.4f, %.4f]" % (i, rmse, expOutput[0], expOutput[1])

for i in range(1, 10000, 100):
    result = fann.run(i)
    rmse = fann.rmse(i, result)
    print "Result: %d, RMSE: %.4f, output=[%.4f, %.4f]" % (i, rmse, result[0], result[1])
