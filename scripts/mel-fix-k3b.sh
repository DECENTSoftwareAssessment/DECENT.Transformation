#!/bin/bash

D=~/TEMP/deployment
T=`basename $1`
T=k3b


cd $D/data/k3b
cp postfamix/model.decent* .
#cp decent2cfa/model.cfa .

cd $D/mel
#java -d64 -Xms1024m -Xmx20g -Xss4096m -jar mel.jar properties/$T > $T.sample.log
java -d64 -Xms1024m -Xmx10g -Xss1024m -jar mel.jar properties/$T > $T.refresh.s1.log

cd $D/data/k3b
mkdir -p decent2cfa/
cp model.cfa decent2cfa/

cd $D/mel
#also skip if needed, also make backup if needed
java -d64 -Xms1024m -Xmx10g -Xss1024m -jar mel.jar properties/$T EXTRA2CFA > $T.refresh.s2.log

#also try with binary or even switch to DB backend..
java -d64 -Xms1024m -Xmx10g -Xss1024m -jar mel.jar properties/$T CFA2DECENTSimple > $T.refresh.s3.log

