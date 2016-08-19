#!/bin/bash

if [[ -z $1 ]]
then
  echo "usage: `basename $0` TARGET STEP"
  exit 1
fi


TARGET=$1
STEP=$2

cd rt
java -Xmx2048m -jar rt.jar properties/$TARGET.properties $STEP > logs/rt.$TARGET.$STEP.log

