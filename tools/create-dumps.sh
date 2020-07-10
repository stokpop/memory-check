#!/usr/bin/env bash

if [[ $# -lt 3 ]] ; then
    echo 'Please provide: [application name] [number of histos] [delay in milliseconds]'
    exit 1
fi

APP_NAME=$1
TIMES=$2
DELAY_MS=$3
DELAY_SEC=$(echo "$DELAY_MS / 1000" | bc)

PID=$(jps | grep $APP_NAME | cut -d' ' -f1)

for i in $(seq 1 $TIMES)
do
    jmap -histo:live $PID > histo.dump.$APP_NAME.$(date +%Y-%m-%dT%H:%M:%S).txt
    sleep $DELAY_SEC
done