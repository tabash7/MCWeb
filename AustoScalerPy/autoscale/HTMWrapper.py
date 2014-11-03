'''
Created on 14/06/2014

@author: nikolay
'''
import datetime
import logging
import time
from autoscale import ModelParams
from nupic.frameworks.opf.modelfactory import ModelFactory
from os.path import os
import shutil
from autoscale.VMMeasurement import VMMeasurement

log = logging.getLogger(__name__)


class HTMWrapper(object):
    """ Wraps a Hierarchical Temporal Memory (HTM) model. """
    
    def __init__(self):
        """Constr."""
        self.model = ModelFactory.create(ModelParams.MODEL_PARAMS)
        self.model.enableInference({'predictedField': 'cpuUtil'})
        self.headers = ["timestamp", "cpuUtil", "ramUtil", "diskUtil", "numUsers"];
    
    def train(self, measurement):
        """
        Trains the underlying HTM with the sample.
        @param measurement: An instance of VMMeasurement. Must not be None. 
        @return: the anomaly score of the measurment.
        """
        assert isinstance(measurement, VMMeasurement), "Invalid measurement: %s" % (measurement)
        
        modelInput = {}

        scale = 1
        modelInput["timestamp"] = measurement.timestamp
        modelInput["numUsers"] = measurement.numberOfUsers()
        modelInput["cpuUtil"] = scale * measurement.cpuUtil()
        modelInput["ramUtil"] = scale * measurement.ramUtil()
#         modelInput["diskUtil"] = scale * measurement.normaliseDiskUtil()
        
        result = self.model.run(modelInput)
        anomalyScore = result.inferences['anomalyScore']
        measurement.anomaly = anomalyScore
        log.info("Training anomaly with [%d, %.5f, %.5f] Anomaly: %.2f ", modelInput["numUsers"], modelInput["cpuUtil"], modelInput["ramUtil"], anomalyScore)
        return anomalyScore
    
    def clone(self):
        """
        Returns a deep copy of this object.
        @return: a deep copy of this object. 
        """
        tmpDirLocation = os.path.expanduser("~/buffer-htm")
        # Make sure the directory exists and it is empty
        if os.path.exists(tmpDirLocation):
            shutil.rmtree(tmpDirLocation)
        #os.makedirs(tmpDirLocation)
        
        # Save the model in the tmp folder
        self.model.save(tmpDirLocation)
        
        # load from tmp location
        newModel = ModelFactory.loadFromCheckpoint(tmpDirLocation)
        result = HTMWrapper()
        result.model = newModel
        return result


def createModel():
    return ModelFactory.create(ModelParams.MODEL_PARAMS)

def runVMAnomaly():
    """For test purposes only!"""
    model = createModel()
    model.enableInference({'predictedField': 'cpuUtil'})
  
    headers = ["timestamp", "cpuUtil", "ramUtil", "diskUtil", "numUsers"];
    records = [[0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.3, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.2, 0.2, 0.2, 100],
        [0.22, 0.3, 0.3, 110],
        [0.24, 0.4, 0.35, 120],
        [0.26, 0.5, 0.45, 130],
        [0.28, 0.6, 0.50, 140],
        [0.3, 0.7, 0.55, 150],
        [0.32, 0.8, 0.60, 160],
        [0.34, 0.9, 0.65, 170],
        [0.36, 0.95, 0.70, 180],
        [0.38, 0.99, 0.65, 190],
        [0.40, 0.99, 0.60, 200],
        [0.2, 0.2, 0.2, 100],
        [0.5, 0.2, 0.2, 100]]
  
    for record in records:
        modelInput = dict(zip(headers, [None] + record))
        modelInput["timestamp"] = datetime.datetime.now()
        
        modelInput["cpuUtil"] = float(modelInput["cpuUtil"])
        modelInput["ramUtil"] = float(modelInput["ramUtil"])
        modelInput["numUsers"] = float(modelInput["numUsers"])
            
        result = model.run(modelInput)
        anomalyScore = result.inferences['anomalyScore']
        log.info("Time [%s]. Record %s, Anomaly score: %f.",
                        result.rawInput["timestamp"],
                        dict(zip(headers[1:], record)),
                        anomalyScore)
        time.sleep(1)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    runVMAnomaly()
  
