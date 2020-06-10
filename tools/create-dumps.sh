#!/usr/bin/env bash

APP_NAME=$1
TIMES=$2
DELAY_MS=$3
DELAY_SEC=$(echo "$DELAY_MS / 1000" | bc)

PID=$(jps | grep $APP_NAME | cut -d' ' -f1)

for i in $(seq 1 $TIMES)
do
    jmap -histo:live $PID > histo.dump.$APP_NAME.$(date +%Y%m%d.%H%M%S).txt
    sleep $DELAY_SEC
done