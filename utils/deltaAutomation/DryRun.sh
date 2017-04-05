#!/usr/bin/env bash


USER_TYPE=$1
INPUTFILE=$2
INPUTFILE="@$INPUTFILE"

if [ $# -eq 2 ]
 then
    curl --url "https://www-qa.tax.service.gov.uk/eeitt-auth/etmp-data/$USER_TYPE/dry-run" --user eeitt-qa-user:gp6uIbyBTevIf/EB --header 'x-requested-with: bar' --data-binary $INPUTFILE -v

    echo  --url "https://www-qa.tax.service.gov.uk/eeitt-auth/etmp-data/$USER_TYPE/dry-run" --user eeitt-qa-user:gp6uIbyBTevIf/EB --header 'x-requested-with: bar' --data-binary $INPUTFILE -v

 else
    echo "Incorrect number of arguments supplied. The format is ./DryRun.sh RECORD_TYPE FULL_PATH_TO_FILE . For example, ./DryRun.sh business-users /home/rajesh/Applications/sample"
fi
