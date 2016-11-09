#!/bin/bash
source merger.conf

START_TIME="$(date +%s)"

TOTAL_POSTINGS_FILE=0
for entry in `ls $inputpath`; do
  if [[ $entry == $postingsfileformat* ]]
  then
    FILES[TOTAL_POSTINGS_FILE]=$entry
    ((TOTAL_POSTINGS_FILE++))
  fi
done

FILES_TO_JOIN=$filestojoin
COUNTER=0

if [[ $FILES_TO_JOIN -lt 2 ]]
then
  echo "ERROR: Files to join should be more than 1"
  exit 1
fi

BASE_COMMAND="join -a1 -a2"
BASE_LEN=${#BASE_COMMAND}
PHASES=0

while [ $COUNTER -lt ${#FILES[@]} ]
do
  COMMAND="$BASE_COMMAND ${FILES[COUNTER]} ${FILES[COUNTER+1]} | $BASE_COMMAND"
  if [[ ! $FILES_TO_JOIN -le 2 ]]
  then
    for ((i=2; i<$FILES_TO_JOIN; i++))
    do
      COMMAND="$COMMAND - ${FILES[COUNTER+i]} | $BASE_COMMAND"
    done
  fi

  COMMAND=${COMMAND:0:${#COMMAND}-($BASE_LEN+3)}

  echo "Starting merging phase#$PHASES ..."
  echo "Runnin command: $COMMAND"

  eval $COMMAND > $outputmergedfileformat'_'$PHASES

  echo "Finished merging phase#$PHASES !!!
  "
  COMMAND=$BASE_COMMAND
  ((COUNTER=COUNTER+FILES_TO_JOIN))
  ((PHASES++))
done

END_TIME="$(date +%s)"

((EXCECUTION_TIME=END_TIME-START_TIME))
echo "Total Exceution time = $EXCECUTION_TIME(s)
Total output files = $PHASES
Total input files = ${#FILES[@]}"
