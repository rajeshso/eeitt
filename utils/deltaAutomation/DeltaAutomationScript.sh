#!/usr/bin/env bash

INPUTFILELOCATION=$1
OUTPUTFILELOCATION=$2
BADFILELOCATION=$3
INPUTFILENAME=$4

if [ $# -eq 4 ]
 then
    java -jar target/scala-2.12/delta-automation-assembly-1.0.jar "${INPUTFILELOCATION}" "${OUTPUTFILELOCATION}" "${BADFILELOCATION}" "${INPUTFILENAME}"
 else
    echo "Incorrect number of arguments supplied. The format is ./DeltaAutomationScript.sh INPUTFILELOCATION OUTPUTFILELOCATION BADFILELOCATION INPUTFILENAME"
fi