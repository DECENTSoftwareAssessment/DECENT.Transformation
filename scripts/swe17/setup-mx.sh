#!/bin/bash

if [[ -z $1 ]]
then
  echo "usage: `basename $0` TARGET [LANGUAGE]"
  exit 1
fi

TARGET=$1

cp "mx/properties/proto.conf" "mx/properties/$TARGET.conf"

sed -i s@PROTO@$TARGET@g "mx/properties/$TARGET.conf"

if [[ ! -z $2 ]]
then
  LANGUAGE=$2
  sed -i "s@LANG=cpp@LANG=$LANGUAGE@g" "mx/properties/$TARGET.conf"
fi

