#!/bin/bash

if [ "$#" -lt 1 ]; then
    echo "Usage: neomel-workflow MODE PROJECT-LOCATION"
    exit 1
fi

D=~/TEMP/deployment/neodata
MD=~/TEMP/deployment/neomel
T=$1
P=$2

M="java -d64 -Xms1024m -Xmx10g -Xss1248m -jar mel.jar"
#java -d64 -Xms1024m -Xmx10g -Xss1248m -jar mel.jar properties/log4j > log4j.decent2arffx.log

function backup {
	echo "  Backup P$1 $2..."
	cd $D/data/$T
	mkdir -p P$1
	tar -cvzf P$1/model.$2.tar.gz model.$2* 
}

function execute {
	mode=$1
	project="$D/$2"
	echo "  Execute $mode on $project..."
	cd $MD
	#`$M $mode $project >> $MD/logs/neo-$project.log`
	$M $mode $project
	#echo "$M $mode $project"
}

execute $T $P

