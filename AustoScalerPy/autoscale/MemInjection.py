'''
Created on 16/06/2014

@author: nikolay
'''
import sys
import time
import gc

def convertMem(mem, fromCode="GB", toCode="MB"):
    indices = ["B", "KB", "MB", "GB", "TB", "PB"]
    fromIdx = indices.index(fromCode.strip().upper())
    toIdx = indices.index(toCode.strip().upper())
    return mem * 1024 ** (fromIdx - toIdx)


data = []

# echo `find /tmp/http_sessions/ -maxdepth 1  -type f ! -size 0 -mmin -0.5 | wc -l`

#while True:
for n in range(1, 100):
    n = n % 15 + (n +3) %7
    
    if len(data) > n:
        del data[n:]
        gc.collect()
    else :
        for i in range(n - len(data)):
            mb = bytearray([])
            for i in range(convertMem(1, fromCode="MB", toCode="B")):
                mb.append(0x00)
            data.append(mb)
    
    dataSize = map(lambda x : sys.getsizeof(x), data)
    
    print n, len(data), sum(dataSize), convertMem(sum(dataSize), fromCode="B", toCode="MB")
    time.sleep(1)
