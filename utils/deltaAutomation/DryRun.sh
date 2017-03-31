#!/usr/bin/env bash

sm --start EEITT_ALL -fo


USER_TYPE=$1
INPUTFILE=$2
INPUTFILE="@$INPUTFILE"

if [ $# -eq 2 ]
 then
    curl --url "http://localhost:9191/eeitt/etmp-data/dry-run/$USER_TYPE" --user dave:davespassword --header 'x-requested-with: bar' --data-binary $INPUTFILE -v

    echo  --url "http://localhost:9191/eeitt/etmp-data/dry-run/$USER_TYPE/" --user dave:davespassword --header 'x-requested-with: bar' --data $INPUTFILE -v

 else
    echo "Incorrect number of arguments supplied. The format is ./DryRun.sh RECORD_TYPE FULL_PATH_TO_FILE . For example, ./DryRun.sh business-users /home/rajesh/Applications/sample"
fi

sm --stop EEITT_ALL