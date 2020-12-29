#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
KI_SHELL=$SCRIPT_DIR/../lib/ki-shell-*.jar

java -jar ${KI_SHELL} $@
