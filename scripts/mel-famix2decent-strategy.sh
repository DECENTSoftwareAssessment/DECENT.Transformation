#!/bin/bash

D=~/TEMP/deployment
T=`basename $1`

cd $D/data/k3b
cp mg2decent/model.decent .
cp mg2decent/model.decentbin .

cd $D/mel
java -d64 -Xms1024m -Xmx20g -Xss4096m -jar mel.jar properties/$T > $T.sample.log

cd $D/data/k3b
mkdir -p $T
mv model.decent $T/
mv model.decentbin $T/
mv model.log $T/