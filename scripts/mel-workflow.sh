#!/bin/bash

if [ "$#" -lt 1 ]; then
    echo "Usage: mel-workflow PROJECT-CONFIGURATION [PHASES]"
    exit 1
fi

D=~/TEMP/deployment
MD=~/TEMP/deployment/mel
T=$1

M="java -d64 -Xms1024m -Xmx10g -Xss1248m -jar mel.jar properties/"
#java -d64 -Xms1024m -Xmx10g -Xss1248m -jar mel.jar properties/log4j > log4j.decent2arffx.log

function backup {
	echo "  Backup P$1 $2..."
	cd $D/data/$T
	mkdir -p P$1
	tar -cvzf P$1/model.$2.tar.gz model.$2* 
}

function execute {
	x=$1
	p=P$x
	P=${!p}
	echo "  Execute $p: $P ..."
	cd $MD
	`$M$T $P >> $MD/logs/$T.log`
}

P0=MG2NORMALIZEDHUNKS
P1=MG2DECENT,FAMIX2DECENT,HITS2DECENT,MG2CFA,DECENT2CFA,EXTRA2CFA,SHARED2CFA
P2=CFA2DECENTSimple
P3=DAG2DECENT
P4=COLLABORATION2DECENT
P5=DELTA2DECENT
P6=DECENT2ARFFx,ARFFx2ARFF
P9=HITS2DECENT
P10=MG2CFA,DECENT2CFA,EXTRA2CFA,SHARED2CFA
P11=BIN2DECENT
P12=DECENT2BIN
P16=binDECENT2ARFFx,binARFFx2ARFF

#rm $MD/logs/$T.log

if [ "$#" -lt 2 ]; then
	t="decent"
	s=1 && execute $s && backup $s $t && 
	s=2 && execute $s && backup $s $t &&
	s=3 && execute $s && backup $s $t &&
	s=4 && execute $s && backup $s $t &&
	s=5 && execute $s && backup $s $t &&
	s=6 && execute $s 
else
	for s in $(echo $2 | tr "," "\n")
	do
		t="decent"
		if [ $s = 6 ]; then
			t="arffx"
		fi
		if [ $s = 0 ]; then
			execute $s
		else 
	  		execute $s && backup $s $t 
		fi
	done
fi

