#!/bin/bash

D=~/TEMP/deployment

#$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-dynamic
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-fixed-20
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-safe
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-dynamic
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-fixed-20
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-safe

$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-dynamic-1000
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-fixed-20-1000
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-bin-safe-1000
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-dynamic-1000
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-fixed-20-1000
$D/mel-famix2decent-strategy.sh $D/mel/properties/k3b-xmi-safe-1000
