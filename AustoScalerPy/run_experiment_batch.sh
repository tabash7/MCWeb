#!/bin/bash
echo "Starting Test Suite:" > ~/out.txt
for h in 250 
do
    for i in 1 
    do
        for lr in "0.001"  
        do
            for e in 2 1   
            do
                printf "\n\n=== Test with $i : $h nodes; learning rate:$lr epochCode:$e \n\n\n" >> ~/out.txt
                echo "$i - $h nodes; learning rate:$lr epochCode:$e"
                python autoscale/Main.py $i $h $lr $e >> ~/out.txt 
                sleep 10m
            done
        done
    done
done
