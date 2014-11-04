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
    """ Wraps a FANN neural network. """
    
    def __init__(self, topology=(3, 250, 2), inputMomentum = 0.05, learning_rate = 0.01, connection_rate = 1):
        """
        Constr.
        @param topology: A vector of integers, specifying the number of neurons in each layer. Must not be None. Must have more than 1 element.
        @param inputMomentum: The training momentum. Must be in the interval [0,1).
        @param learning_rate: The learning rate. Must be in the interval [0,1).
        @param connection_rate: The FANN connection rate. Must be an integer greater or equal to 1.
        """
        
        assert topology is not None and len(topology) > 1, "Topology %s is invalid" % str(topology) 
        assert reduce(lambda x,y: x and y, map(lambda z : isinstance(z, int) and z > 0, topology)), "Topology %s contains invalid elements" % str(topology)
        assert inputMomentum is not None and 0 <= inputMomentum < 1, "Input momentum %s is invalid" % inputMomentum
        assert learning_rate is not None and 0 <= learning_rate < 1, "Learning rate %s is invalid" % learning_rate 
        assert connection_rate is not None and connection_rate >= 1, "Connection rate %s is invalid" % connection_rate 
        
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
        

    def train(self, n, expOutput, trainTimes = 1, revert=False, maxRMSE=None):
        """
        Trains the underlying neural network, until trainTimes iterations have been performed, or the RMSE has been achieved.
        @param n: Number of users. Must not be None. Must be non-negative integer.
        @param expOutput: The expected output from the NN. Must be of appropriate size. Must not be None.
        @param trainTimes: How many training iterations to perform. An integer, greater or equal to 1. Must not be None. 
        @param revert: Boolean flag, whether to revert the changes to the NN after the training.
        @param maxRMSE: If not None, trains until the RMSE is achieved. May be None. May not be 0 or negative.
        @return: The RMSE after the training.
        """
        
        assert n is not None and n >= 0, "Invalid n: %s" % n
        assert expOutput is not None and len(expOutput) == self.topology[-1], "Invalid Expected Output: %s" % str(expOutput)
        assert trainTimes is not None and isinstance(trainTimes, int) and trainTimes > 0, "Invalid trainTimes: %s" % trainTimes
        assert revert is not None and isinstance(trainTimes, bool), "Invalid revert param: %s" % revert
        assert maxRMSE is None or maxRMSE > 0, "Invalid maxRMSE param: %s" % maxRMSE
        
        inp = self._convertInput(n)
        assert len(inp) == self.topology[0], "Input size: {0}, expected size:{1}".format(len(inp), self.topology[0])
        assert len(expOutput) == self.topology[-1], "Output size: {0}, expected size:{1}".format(len(expOutput), self.topology[-1])
        log.info("Training for %s users %s times with input:%s and output:%s", n, trainTimes, str(inp), str(expOutput))
        
        bufferAnnFile = os.path.expanduser("~/buffer-ann.txt")
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
        """
        Runs the underlying neural network.
        @param n: Number of users. Must not be None. Must be non-negative integer.
        @return: The output layer as a colleciton.
        """
        assert n is not None and n >= 0, "Invalid n: %s" % n
        return self.ann.run(self._convertInput(n))
    
    def rmse(self, n, expectedOutput):
        """
        Computes the RMSE for the given number of users and expected output.
        @param n: Number of users. Must not be None. Must be non-negative integer.
        @param expOutput: The expected output from the NN. Must be of appropriate size. Must not be None.
        @return: the RMSE for the given number of users and expected output.
        """
        assert n is not None and n >= 0, "Invalid n: %s" % n
        assert expectedOutput is not None and len(expectedOutput) == self.topology[-1], "Invalid Expected Output: %s" % str(expectedOutput)
        return rmse(self.run(n), expectedOutput)

    def config(self, momentum = None, learning_rate = None):
        """
        Reconfigures the neural network's momentum and learning_rate.
        @param momentum: The new momentum. If None, the momentum is not changed. Must be in the range [0,1)  
        @param learning_rate: The new learning_rate. If None, the learning_rate is not changed. Must be in the range [0,1)
        """
        assert momentum is None or 0 <= momentum < 1, "Momentum %s, is invalid" % (momentum)
        assert learning_rate is None or 0 <= learning_rate < 1, "Learning rate %s, is invalid" % (learning_rate)
        
        if momentum is not None:
            self.momentum = momentum
            self.ann.set_learning_momentum(self.momentum)
            
        if learning_rate is not None:
            self.learning_rate = learning_rate
            self.ann.set_learning_rate(learning_rate)

        log.info("Momentum %.4f Learning Rate %.4f", self.momentum, self.learning_rate)


    def _convertInput(self, n):
        result = [n]
        simplelog = lambda x : math.log(x) if x >=1 else x 
        if self.topology[0] > 1:
            result.append(simplelog(n))
        if self.topology[0] > 2:
            result.append(simplelog(simplelog(n)))
            
        assert len(result) == self.topology[0], "Input size: %s, expected size:%s" % (len(result), self.topology[0])
        return result
