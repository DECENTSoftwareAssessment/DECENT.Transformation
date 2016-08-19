#!/bin/bash

# run with
# ./mx_automator.sh rekonq.conf 4 2742 > mx_rekonq.log 2>&1
# to get output into log file
# 
# for single process with redirection run
# java -jar mx.jar rekonq.conf > rekonq.log 2>&1
#
# for global offset in multiprocess / cluster operation starting at 1000 processing the next 500
# mx_automator.sh rekonq.conf 4 500 1000 
# i.e.
# mx_automator.sh CONFIG WORKERS LIMIT GLOBAL_OFFSET 
#

project="${1##*/}"
project="${project%.*}"
log=logs/$project
mkdir -p $log

conf=$(readlink -f $1)	
workers=$2
number_of_revisions=$3
#number_of_revisions=2742
revisions_each=$((number_of_revisions / workers))
mode="infamix"
#mode="clones"
global_offset=0
if [ $4 ]
then 
    global_offset=$4
fi

# init and process first revision
if [ "$mode" != "infamix" ]; then
	java -jar mx/mx.jar $mode $conf 0 1 setup
fi

echo "Revisions found: "$number_of_revisions
echo "Splitting into "$workers" packages with ~"$revisions_each" revisions each."

# setting overall timestamp for benchmarking
overall_start=$(date +%s%N | cut -b1-13)

# split revisions into work packages. atm this is done lineally with work packages equally sized.
# in the future different distribution schemas (like logarithmical or exponential) may perfom better
# depending on the size differences between single revisions
for i in $(seq $workers); do
  offset=$(((i-1) * $revisions_each + $global_offset))
  if [[ $i -lt $workers ]]; then
    limit=$(($revisions_each))
  else
    limit=$(($number_of_revisions-(($i-1) * $revisions_each)))
  fi
  #echo $i $offset $limit
  # prepare and submit command to grid via qsub. each worker gets indepent stdout and errorlog
  # command="cd $(pwd); java -classpath $classes com.dmay.ba.metrics.MetricsExtractor $conf -revisions $package"
  # echo $command | qsub -N $$"-"$i"-"$current -o $package".out" -e $package".log" > /dev/null
  # echo " java -jar mx.jar $conf $offset $limit &"

# standard configuration
#  java -jar mx.jar $mode $conf $offset $limit & #>> mx_rekonq.log 2>&1 

# cluster configuration
    command="java -jar mx/mx.jar $mode $conf $offset $limit "
	if [ "$mode" == "infamix" ]; then
	    command="./setupX-local.sh $mode $conf $offset $limit "
    fi
    mx_pack_name="pack-$offset-$limit"
    $command > $log/$mx_pack_name.log
    #echo $command | qsub -N $project.$mx_pack_name -o $log/$mx_pack_name".out" -e $log/$mx_pack_name".log" -q verylong -k oe
#  java -jar mx.jar $conf $i 1 &
done

for job in `jobs -p`
do
  wait $job
done

overall_end=$(date +%s%N | cut -b1-13)

# Time interval in milliseconds
T=$((overall_end-overall_start))
# Seconds
S=$((T/1000))
# Milliseconds
M=$((T%1000))

printf "Total Duration (DD:HH:MM:SS:mmm): %02d:%02d:%02d:%02d.%03d\n" "$((S/86400))" "$((S/3600%24))" "$((S/60%60))" "$((S%60))" "${M}"
echo "$T ms"
