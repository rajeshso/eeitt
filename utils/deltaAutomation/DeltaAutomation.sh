#!/usr/bin/env bash

INPUTFILELOCATION=$1
OUTPUTFILELOCATION=$2
BADFILELOCATION=$3
INPUTFILENAME=$4
DELTAAUTO_HOME=$(cd $(dirname $0);echo $PWD)
DELTAAUTO_HOME=${DELTAAUTO_HOME//"/bin"/}

export DELTAAUTO_HOME=${DELTAAUTO_HOME}

if [ $# -eq 4 ]
 then
    java -jar target/scala-2.12/delta-automation-assembly-1.0.jar "${INPUTFILELOCATION}" "${OUTPUTFILELOCATION}" "${BADFILELOCATION}" "${INPUTFILENAME}"
 else
   echo "Incorrect number of arguments supplied. The format is ./DeltaAutomation.sh INPUTFILELOCATION OUTPUTFILELOCATION BADFILELOCATION INPUTFILENAME"
fi