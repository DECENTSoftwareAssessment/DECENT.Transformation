#!/bin/bash

TARGETLINE="Xvfb :5 -ac -screen 0 1024x768x8"
TARGETPID=`pgrep -f "$TARGETLINE"`
if [ $TARGETPID ]
then 
  kill $TARGETPID
fi

$TARGETLINE &>/dev/null &
sleep 1
#Xvfb :1 -ac -screen 0 1024x768x8 & 
export DISPLAY=:5
echo `hostname`"$DISPLAY"

java -jar mx/mx.jar $1 $2 $3 $4
