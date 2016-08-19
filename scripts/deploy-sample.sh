#!/bin/bash

LOCATION=$1
TARGET="~/Resources/git/"
#echo $LOCATION
PARENT=`dirname $LOCATION`
NAME=`basename $LOCATION`
DBNAME=mg_auto_$NAME
DDDBNAME=dd_auto_$NAME

#DBNAME=$NAME
TS=`date +"%m-%d-%Y_%H-%M"`
ANAME=$NAME-$TS
TNAME=auto_$NAME
BNAME=$TNAME-backup-$TS
#TNAME=$NAME

LANGUAGE=java

cd $PARENT

echo "Processing $NAME.."

echo "Compress.."
tar -czf $ANAME.tar.gz $NAME 

echo "Transmit.."
scp -P 7717 $ANAME.tar.gz localhost:$TARGET 

echo "Cleanup and extract.."
ssh -p 7717 localhost "cd $TARGET && if [ -d $TNAME ]; then mv $TNAME $BNAME; fi && mkdir $TNAME && tar -xzf $ANAME.tar.gz -C $TNAME --strip-components=1"

echo "Mine VCS.."
ssh -p 7717 localhost "cd $TARGET/$TNAME && source ~/.profile && cvsrdb $DBNAME && cvsadb $DBNAME && cvsautomate $DBNAME ."

echo "Setup RT.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./setup-rt.sh $TNAME"

echo "Run RT-MG.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./run-rt.sh $TNAME MG"

echo "Setup MX.."
#ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./setup-mx.sh $TNAME $LANGUAGE"

echo "Run MX-FAMIX.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./mx-automator-local.sh mx/properties/$TNAME.conf 1 0 > mx/logs/$TNAME.log"

echo "Run RT-FAMIX.."
ssh -p 7717 localhost "cd TEMP/deployment && if [ -d ~/Resources/results/$TNAME/famix ]; then mv ~/Resources/results/$TNAME/famix ~/TEMP/deployment/data/$TNAME/; fi && source ~/.profile && ./run-rt.sh $TNAME FAMIX"

echo "Run MX-DUDE.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && cvsrdb $DDDBNAME && cvsadb $DDDBNAME && ./mx-automator-local.sh mx/properties/$TNAME.conf 1 0 0 clones > mx/logs/$TNAME_clones.log"

echo "Run RT-DUDE.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./run-rt.sh $TNAME DUDE"

echo "Run RT-DAG.."
ssh -p 7717 localhost "cd TEMP/deployment && source ~/.profile && ./run-rt.sh $TNAME DAG"

#MEL?

echo "Compress results.."
ssh -p 7717 localhost "cd TEMP/deployment/data/ && tar -czf $TNAME.tar.gz $TNAME"

echo "Retrieve results.."
scp -P 7717 localhost:~/TEMP/deployment/data/$TNAME.tar.gz ~/Dev/workspaces/emf/DECENT.data/input/ 

echo "Extract results.."
cd ~/Dev/workspaces/emf/DECENT.data/input/ && if [ -d $TNAME ]; then mv $TNAME $BNAME; fi && tar -xzf $TNAME.tar.gz 
