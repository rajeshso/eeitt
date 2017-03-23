#!/usr/bin/env bash

HOSTURL="http://localhost:9190/eeitt-auth/etmp-data/"
USER_TYPE="business-users"
#USER_TYPE="agent-users"
DRYRUN="dry-run"
URL=$HOSTURL$USER_TYPE"/"$DRYRUN
OPERATOR_NAME=$1
OPERATOR_PASSWORD=$2
OPERATOR=$OPERATOR_NAME:$OPERATOR_PASSWORD
HEADER="'x-requested-with: bar'"
INPUTFILE=$3
INPUTFILE="'@$INPUTFILE'"

if [ $# -eq 3 ]
 then
    curl -v --url $URL --user dave:davespassword --header $HEADER --data-binary $INPUTFILE
 else
    echo "Incorrect number of arguments supplied. The format is ./DryRun.sh OPERATORNAME OPERATORPASSWORD INPUTFILENAME . For example, ./DryRun.sh dave davepassword /home/rajesh/Applications/sample"
fi