#!/bin/bash

##### NOTE - tested with sysstat version 10.2.0 and vmstat from procps-ng 3.3.9

# Make sure all time vars are formatted appropriately in the same way on all servers
# Otherwise our parsing will not work on some systems
export LC_TIME=en_AU.UTF-8 &> /dev/null


## ===================== Reusable Utility Functions =====================
function fileSizeInMB { 
    sizeMB=`stat --printf="%s" $1`
    sizeMB=$((sizeMB / 1024 / 1024))
    echo $sizeMB
}

# Returns the index of the first arguement in the list provided as a second arg. Non-zero based indexing
function firstIndexOf {
    value=$1
    args=$@

    i=1
    for arg in $@
    do
        if [ $i -gt "1" ] && [ "$arg" = "${value}" ]; then
            echo $((i-1))
            break
        fi
        i=$((i+1))
    done 
}

# Resets the content of a file if its size is greater (in terms of megabytes) than specified
function resetIfFullOrDeleted { 
    aFile=$1
    maxSizeMb=$2
    archiveFlag=$3
    patternArg=$4
    
    if [ ! -f $aFile ] 
    then 
        printf "$patternArg" ${@:5} > $aFile
    else
        sizeMB=`fileSizeInMB $aFile`
        if [ $sizeMB -gt $maxSizeMb ]
        then
            if [ $archiveFlag = "True" ]
            then
                fileName="${aFile%.*}"
                zipFile=$fileName`date -R`.zip
                zip -j "$zipFile" $aFile
            fi
            printf "$patternArg" ${@:5} > $aFile
        fi
    fi
}

# Returns the number of presently logger users:
function numberOfUsers { 
    echo `find /tmp/http_sessions/ -maxdepth 1  -type f ! -size 0 -mmin -0.5 | wc -l`
}
## ===================== ===================== ===================== =====================

# Kill all running instances of this script, without the current one:
echo "Starting monitor.sh with PID:" $$ >> ~/monitor.log
for pid in `ps -ef | grep "monitor.sh" | grep -v color | awk '{ print $2 }'` 
do
    if [ $$ != $pid ] && [ $PPID != $pid ]
    then 
        echo `date`" From process $$ killing process: $pid" &>> ~/monitor.log
        kill -9 $pid &>> ~/monitor.log
    fi 
done

# Dynamically retrieve the indicies of the columns we're interested in, as they may vary on different systems
mpstatHeader=(`mpstat 1 1 | grep "%idle"`)
iowaitMpstatIdx=`firstIndexOf "%iowait" ${mpstatHeader[@]}`
stealMpstatIdx=`firstIndexOf "%steal" ${mpstatHeader[@]}`
idleMpstatIdx=`firstIndexOf "%idle" ${mpstatHeader[@]}`

vmstatHeader=(`vmstat -n -a -S k 1 1 | grep active`)
activeVmstatIdx=`firstIndexOf "active" ${vmstatHeader[@]}`

sarDiskHeader=(`sar -d 1 1 | grep DEV | grep -i average`)
diskUtilIdx=`firstIndexOf "%util" ${sarDiskHeader[@]}`

sarNicHeader=(`sar -n DEV 1 1 | grep IFACE | grep -i average`)
nicUtilIdx=`firstIndexOf "%ifutil" ${sarNicHeader[@]}`

sarSocketsHeader=(`sar -n SOCK 1 1 | grep tcpsck`)
tcpsckIdx=`firstIndexOf "tcpsck" ${sarSocketsHeader[@]}`
tcptwIdx=`firstIndexOf "tcp-tw" ${sarSocketsHeader[@]}`

# Intermediate files greater than this will be cleaned
maxFileSizeMb=10

# Intervals between measurements, and how many measurmenets to take
measInterval=1
measTimes=5

# Intermediate and result files
mpstatResFile=~/mpstatResults.txt
vmstatResFile=~/vmstatResults.txt
sarDiskUtilFile=~/sar-disk-util.txt
sarNicUtilFile=~/sar-nic-util.txt
sarSocketsFile=~/sar-sockets.txt

tmpFiles=($mpstatResFile $vmstatResFile $sarDiskUtilFile $sarNicUtilFile $sarSocketsFile)

resultFile=~/monitoring-data.csv

# Pattern and header
export pattern="%-15s; %-15s; %-15s; %-15s; %-15s; %-15s; %-15s; %-15s; %-15s; %-15s \n"
headers=("Time" "cpuCapacityMhz" "cpuIOWaitPerc" "cpuStealPerc" "cpuIdlePerc" "ramInKb" "activeMemInKb" "diskUtilPerc" "nicUtilPerc" "numUsers")
rm $resultFile &>> ~/monitor.log

## ===================== Collect and record monitoring data in an infinite loop  =====================
while :
do
    # Check if files have not become too big... if so empty them
    for f in ${tmpFiles[@]}
    do 
        resetIfFullOrDeleted $f $maxFileSizeMb "False" "" ""
    done
    resetIfFullOrDeleted $resultFile $maxFileSizeMb "True" "$pattern" ${headers[@]}

    # Get the CPU and RAM capacities from /proc/cpuinfo and /proc/meminfo  
    cpuFreqs=(`grep -i "cpu mhz" /proc/cpuinfo | awk 'BEGIN { FS=":" }  { print $2 }'`)
    cpuFreqs=`echo ${cpuFreqs[@]}`
    cpuFreqsSum=`grep -i "cpu mhz" /proc/cpuinfo | awk 'BEGIN { FS=":" }  { print $2 }' | awk '{sum+=$1} END { print sum }'`
    ramInKb=`grep -i "memtotal" /proc/meminfo | awk 'BEGIN { FS=":" }  { print $2 }' | awk ' { print $1}'`

    # Number of users at the period start 
    numberOfUsersAtStart=`numberOfUsers` 

    ## == == Collect monitoring data in intermediate files and variables asynchronously ...
    
    # Collect the average %iowait %steal %idle
    mpstat $measInterval $measTimes | grep -i average | awk ' {print $'$iowaitMpstatIdx' " " $'$stealMpstatIdx' " " $'$idleMpstatIdx} >> $mpstatResFile &
    mpstatPid=$!

    # Collect the average active memory
    vmstat -n -a -S k $measInterval $measTimes | egrep -v "memory|free" | awk '{print $'$activeVmstatIdx'}' | awk '{sum+=$1} END { print int(sum/NR)}' >> $vmstatResFile &
    vmstatPid=$!

    # Collect the max average disk utilisation from all disks
    sar -d $measInterval $measTimes | grep -i average | grep -v DEV | awk '{print $'$diskUtilIdx'}' | awk 'NR == 1 {max=$1} $1 >= max {max = $1} END { print max }' >> $sarDiskUtilFile &
    sarDiskPid=$!

    # Collect the max average NIC utilisation from all network cards
    sar -n DEV $measInterval $measTimes | grep -i average | grep -v IFACE | awk '{print $'$nicUtilIdx'}' | awk 'NR == 1 {max=$1} $1 >= max {max = $1} END { print max }' >> $sarNicUtilFile &
    sarNicPid=$!

    # Collect the average numbers of used sockets (tcpsck) and sockets in TIME_WAIT state (tcp-tw)
    #sar -n SOCK $measInterval $measTimes | grep -i average | awk '{print $'$tcpsckIdx' " " $'$tcptwIdx'}' >> $sarSocketsFile &
    #sarSocketsPid=$!

    # Wait for them to complete
    for pid in $mpstatPid $vmstatPid $sarDiskPid $sarNicPid #$sarSocketsPid
    do
        wait $pid
    done

    # Read the data recorded in the intermediate files
    read cpuIOWait cpuSteal cpuIdle < <(tail -n 1 $mpstatResFile)
    read activeMemInKb < <(tail -n 1 $vmstatResFile)
    read diskUtil < <(tail -n 1 $sarDiskUtilFile)
    read nickUtil < <(tail -n 1 $sarNicUtilFile)
    #read tcpsck tcptw < <(tail -n 1 $sarSocketsFile)

    # Number of users at the period end 
    numberOfUsersAtEnd=`numberOfUsers`
    #numUsers=$((tcpsck - tcptw))
    numUsers=$(( (numberOfUsersAtEnd + numberOfUsersAtEnd) / 2 ))

    ## Debugging Info...
    #echo "cpuIOWait cpuSteal cpuIdle ->" $cpuIOWait $cpuSteal $cpuIdle
    #echo "activeMemInKb ->" $activeMemInKb
    #echo "diskUtil ->" $diskUtil
    #echo "nickUtil ->" $nickUtil
    #echo "tcpsck tcptw ->" $tcpsck $tcptw

    # Prints a formatted CSV
    printf "$pattern" `date +"%T"` $cpuFreqsSum $cpuIOWait $cpuSteal $cpuIdle $ramInKb $activeMemInKb $diskUtil $nickUtil $numUsers >> $resultFile
done
## ===================== ===================== ===================== =====================

