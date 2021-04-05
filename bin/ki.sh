#!/usr/bin/env bash

SCRIPT_DIR="$( dirname $(realpath "$0") )"
KI_SHELL=$SCRIPT_DIR/../lib/ki-shell.jar

java -jar ${KI_SHELL} $@
