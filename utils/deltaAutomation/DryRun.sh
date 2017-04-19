#!/usr/bin/env bash


USER_TYPE=$1
INPUTFILE="$2"
INPUTFILE="@$INPUTFILE"

curl -s -S --url "https://www-qa.tax.service.gov.uk/eeitt-auth/etmp-data/$USER_TYPE/dry-run" --user eeitt-qa-user:gp6uIbyBTevIf/EB --header 'x-requested-with: bar' --data-binary "$INPUTFILE"
