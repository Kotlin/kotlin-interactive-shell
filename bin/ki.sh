#!/usr/bin/env bash

command -v realpath &> /dev/null || realpath() {
  SCRIPT_PATH=$( [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}" )
  if [[ -L $SCRIPT_PATH ]];
  then
    readlink $SCRIPT_PATH
  else
    echo $SCRIPT_PATH
  fi
}

SCRIPT_DIR="$( dirname $(realpath "$0") )"
KI_SHELL=$SCRIPT_DIR/../lib/ki-shell.jar

java -jar ${KI_SHELL} $@
