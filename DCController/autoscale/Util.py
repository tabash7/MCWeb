'''
Created on 05/06/2014

@author: nikolay
'''
import subprocess
from __builtin__ import ValueError
import logging
import math
from time import strftime
import os

log = logging.getLogger(__name__)

def extractVal(value):
    """
    Converts the provided string value to an int, float or string if possible, in this order.
    @param value: the value to convert. Must not be None.
    """
    assert value is not None, "Value is None"
    
    trimmed = value.strip()
    try:
        return int(trimmed)
    except ValueError:
        try:
            return float(trimmed)
        except ValueError:
            return str(trimmed)

def convertMem(mem, fromCode="GB", toCode="MB"):
    """
    Converts the specified memory quantity from one unit to another. Supported units are ["B", "KB", "MB", "GB", "TB", "PB"].
    @param mem: a memory measurement. Must not be None. Must not be negative.
    @param fromCode: the code of the source measurement unit. Must be one of the above.
    @param toCode: the code of the result measurement unit. Must be one of the above.
    @return: the resulted quantity in the destination unit.   
    """
    assert mem is not None and (isinstance(mem, int) or isinstance(mem, float)) and mem >= 0, "Invalid memory: " % (mem)
    
    indices = ["B", "KB", "MB", "GB", "TB", "PB"]
    assert fromCode is not None and fromCode.strip().upper() in indices, "Invalid from code: %s" % (fromCode)
    assert toCode is not None and toCode.strip().upper() in indices, "Invalid to code: %s" % (toCode)
    
    fromIdx = indices.index(fromCode.strip().upper())
    toIdx = indices.index(toCode.strip().upper())
    return mem * 1024 ** (fromIdx - toIdx)

def formatOutput(output):
    """
    Merges the specified output lines into a single string.
    @param output: a collection of strings. Must not be None.
    @return: a merged string of the specified output lines.
    """
    assert output is not None, "Output is None"
    return "\n" + " ".join(output)

def execLocal(command):
    """
    Attempts to execute the command on the local system.
    @param command: the command to execute. Must not be None.  
    @raise ValueError: fi the command fails. 
    @return: a list of ouput lines.
    """
    assert command is not None, "Output is None"
    
    p = subprocess.Popen(command, stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True)
    log.info("Running \"{0}\"".format(command))
    output, err = p.communicate()
    rc = p.returncode
    if rc != 0:
        raise ValueError("Command {0} failed with exit code {1}. Error Message is: \n{2}".format(command, str(rc), err))
    
    errors = filterEmptyStrings(err.split("\n"))
    if errors:
        log.warning("Error messages encountered when executing \"{0}\", messages: {1}".format(command, formatOutput(errors)))
    
    # Return lists of lines, so that we are consistent with paramiko 
    return filterEmptyStrings(output.split("\n")) 

def filterEmptyStrings(collection):
    """
    Returns a sub-collection of the specified one, whose elements are not empty strings.
    @param collection: a collection. Must not be None.
    @return: a sub-collection of the specified one, whose elements are not empty strings.    
    """
    assert collection is not None, "Collection is None"
    return filter(lambda s: str(s).strip() != "", collection)

def formatCurrTime(fmt="%H:%M:%S"):
    """
    Returns the current time, formatted in accordance with the specified format.
    @param fmt: the format string. Must not be None.
    @return: the current time, formatted in accordance with the specified format.  
    """
    assert fmt is not None, "The format is None"
    return strftime(fmt)

def statHeader(trainingResFile, overwrite=True):
    with open(trainingResFile, "a+" if not overwrite else "w") as f:
        txt = "%-10s; %-8s; %-12s; %-5s; %-5s; %-6s; %-10s; %-10s; %-10s; %-10s; %-10s; %-10s; %-8s; %-10s; %-10s; %-10s; %-10s; %-10s; %-10s; %-10s; " \
        % ("Time",
           "Name",
           "Type",
           "N",
           "K",
           "Users",
           "NormCPU",
           "NormRAM",
           "Anomaly",
           "RMSEpre",
           "RunCPU",
           "RunRAM",
           "Epochs",
           "Mom",
           "LR",
           "LR2",
           "AnnP",
           "RMSE-P",
           "Ad-P",
           "RMSE-AV" )
        if not overwrite:
            f.write("\n")
            
        f.write(txt+"\n")
        
def statLine(trainingResFile, values):
    with open(trainingResFile, "a+") as f:
        txt = "%-10s; %-8s; %-12s; %-5d; %-5d; %-6d; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-8d; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; %-10.5f; " \
        % values
        f.write(txt+"\n")


def printTest(trainingStatFile, fann, overwrite=False):
    with open(trainingStatFile, "a+" if not overwrite else "w") as f:
        f.write("\n\n=== === === === "+formatCurrTime()+" === === === === ===\n")
        if fann is not None:
            for i in range(15, 200, 5):
                result = fann.run(int(i))
                txt = "%-10s: %.5d -> [CPU=%.5f, RAM=%.5f]" % (formatCurrTime(), i, result[0], result[1])
                print txt
                f.write(txt + "\n")

def rmse(actual, expected):
    diff= map(lambda a: a**2, [x - y for x, y in zip(actual, expected)])
    mseRun = sum(diff) / float(len(diff))
    return math.sqrt(mseRun)

def sigmoid(x):
    return 1 / (1 + math.exp(-x))

def nextLearningRate(accuracyGain, prevLR, minLR=0.01, maxLR=0.6):
    return min (maxLR, max(minLR, prevLR * 2 * sigmoid( accuracyGain ) ) )
    
def nextMomentum(accuracyGain, prevMomentum, minMomentum=0.05, maxMomentum=0.6):
    return min (maxMomentum, max(minMomentum, prevMomentum) ) 

def nextEpoch(lrk, lr, epochCode = 1):
    nRuns = 0
    if epochCode == 1:
        nRuns = int(round(math.log(lrk / lr) + 1))
    elif epochCode == 2:
        nRuns = int(round(lrk / lr))
    else:
        nRuns = 1
    return nRuns

def getLrk(k, lr, rmsePart, annPart, epochCode = 1):
    return lr if epochCode == 4 else min(0.3, rmsePart*annPart* max(lr, sigmoid(- math.sqrt(k)) )) #0.005            

def getMk(k, lr, lrk, epochCode = 1):
    return 0.9 if epochCode == 4 else min(0.9, lr / lrk )

def sign(x):
    return x if x >= 0 else -x

def baseFolder():
    currentScriptPath = os.path.dirname(os.path.abspath(__file__))
    return os.path.dirname(os.path.dirname(currentScriptPath))

def scriptPath(scriptFile):
    assert scriptFile is not None, "Script file is None"
    return os.path.join(baseFolder(), "ShellScripts", scriptFile)

