#!/usr/bin/env bash

KI_VERSION=0.3.0-SNAPSHOT

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
KI_SHELL=$SCRIPT_DIR/../lib/ki-shell-${KI_VERSION}.jar

java -jar ${KI_SHELL} $@
