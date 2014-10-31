'''
Created on 04/06/2014

@author: nikolay
'''
from pyfann import libfann
import math
import random

##########################################
samples = 10000

hiddenNeurons = 100
learnAlpha = 0.01
inputMomentum = 0.05

def f(k):
    r = random.random()
    return k * 0.01 + r/1000 * (-1 ** int(0.5 + r))

def sample(k):
    simplelog = lambda x : math.log(x) if x >=1 else x 
    return [k, simplelog(k), simplelog(simplelog(k))]
    
def printTest(net):
    for k in [5, 10, 45, 60, 70, 90, 150]:
        print k, net.run(sample(k))
##########################################

connection_rate = 1
learning_rate = learnAlpha

num_input = len(sample(5))
num_hidden = hiddenNeurons
num_output = 4

desired_error = 0.0001
max_iterations = 10
iterations_between_reports = 1000
ann = libfann.neural_net()
ann.create_sparse_array(connection_rate, (num_input, num_hidden, num_output))

ann.set_learning_rate(learning_rate)
ann.set_learning_momentum(inputMomentum)
ann.set_activation_function_hidden(libfann.SIGMOID)
ann.set_activation_function_output(libfann.LINEAR)
ann.set_training_algorithm(libfann.TRAIN_INCREMENTAL)


for i in range(1, samples):
    if i % (samples/2) == 0:
        printTest(ann)
        print "== == == == == == == == == == \n\n"
    k = 1 + i % 99
    inp = sample(k)
    output = [f(k), f(k), f(k), f(k)]
    ann.reset_MSE()
    ann.train(inp, output)
    err = ann.get_MSE()
    if i % 1000 == 0:
        print "----->", err
 
printTest(ann)
