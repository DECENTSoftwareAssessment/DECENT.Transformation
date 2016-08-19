#!/bin/bash

if [[ -z $1 ]]
then
  echo "usage: `basename $0` TARGET"
  exit 1
fi

TARGET=$1
BTARGET=$TARGET.backup.`date +%s`
if [ -d "data/$TARGET" ]; then mv "data/$TARGET" "data/$BTARGET"; fi

cp -r "data/proto" "data/$TARGET"
grep -rl proto "data/$TARGET" | xargs sed -i s@proto@$TARGET@g

cp rt/properties/proto.properties "rt/properties/$TARGET.properties"
sed -i s@proto@$TARGET@g "rt/properties/$TARGET.properties"