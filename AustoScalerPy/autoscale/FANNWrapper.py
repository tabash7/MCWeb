'''
Created on 10/06/2014

@author: nikolay
'''
from pyfann import libfann
import math
import logging
from os.path import os
from autoscale.Util import rmse


log = logging.getLogger(__name__)


class FANNWrapper(object):
    
    def __init__(self, topology=(3, 250, 2), inputMomentum = 0.05, learning_rate = 0.01, connection_rate = 1):
        self.topology = topology
        self.momentum = inputMomentum
        self.learning_rate = learning_rate
        self.connection_rate = connection_rate
        
        self.ann = libfann.neural_net()
        self.ann.create_sparse_array(connection_rate, topology)
        
        self.ann.set_learning_rate(learning_rate)
        self.ann.set_learning_momentum(inputMomentum)
        self.ann.set_activation_function_hidden(libfann.SIGMOID)
        self.ann.set_activation_function_output(libfann.LINEAR)
        self.ann.set_training_algorithm(libfann.TRAIN_INCREMENTAL)
        self.ann.randomize_weights(-0.1, 0.1)
        
        self.proportion = 0

    def train(self, n, expOutput, trainTimes = 1, revert=False, maxRMSE=None):
        bufferAnnFile = os.path.expanduser("~/buffer-ann.txt")
        inp = self._convertInput(n)
        assert len(inp) == self.topology[0], "Input size: {0}, expected size:{1}".format(len(inp), self.topology[0])
        assert len(expOutput) == self.topology[-1], "Output size: {0}, expected size:{1}".format(len(expOutput), self.topology[-1])
        log.info("Training for {0} users {1} times with input:{2} and output:{3}".format(str(n), str(trainTimes), str(inp), str(expOutput)))
        
        if revert:
            log.debug("Saving FANN's state to {0}".format(bufferAnnFile))
            self.ann.save(bufferAnnFile)
        
        for _ in range(trainTimes):
            rmse = self.rmse(n, expOutput)
            if (maxRMSE is not None) and (rmse < maxRMSE):
                break
            self.ann.train(inp, expOutput)
        
        result = self.rmse(n, expOutput)
        
        if revert:
            log.debug("Restoring FANN's state from {0}".format(bufferAnnFile))
            self.ann = libfann.neural_net()
            self.ann.create_from_file(bufferAnnFile)
        
        return result
        
    def run(self, n):
        return self.ann.run(self._convertInput(n))
    
    def rmse(self, n, expectedOutput):
        return rmse(self.run(n), expectedOutput)

    def config(self, momentum = None, learning_rate = None):
        assert momentum is None or 0 <= momentum < 1, "Momentum {0}, must be in [0,1)".format(momentum)
        assert learning_rate is None or 0 <= learning_rate < 1, "Learning rate {0}, must be in [0,1)".format(learning_rate)
        
        if momentum is not None:
            self.momentum = momentum
            self.ann.set_learning_momentum(self.momentum)
            
        if learning_rate is not None:
            self.learning_rate = learning_rate
            self.ann.set_learning_rate(learning_rate)

        log.info("Momentum %.4f Learning Rate %.4f" % (self.momentum, self.learning_rate) )


    def _convertInput(self, n):
        k = n
#         assert self.proportion > 0, "Propotion must not be 0"
        result = [ k]
        simplelog = lambda x : math.log(x) if x >=1 else x 
        if self.topology[0] > 1:
            result.append(simplelog(k))
        if self.topology[0] > 2:
            result.append(simplelog(simplelog(k)))
            
        assert len(result) == self.topology[0], "Input size: {0}, expected size:{1}".format(len(result), self.topology[0])
        return result
