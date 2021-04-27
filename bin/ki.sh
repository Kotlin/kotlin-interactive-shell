#!/usr/bin/env bash

if command -v java &>/dev/null; then
  _JAVA=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
  _JAVA="$JAVA_HOME/bin/java"
else
  echo "java not found"
  exit 1
fi

if [[ "$_JAVA" ]]; then
  JAVA_VERSION=$("$_JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2}')
  if [[ "$JAVA_VERSION" =~ ^1\.[0-8]+ ]]; then
    if [[ "$JAVA_VERSION" =~ ^1\.[0-7]+ ]]; then
      echo "java has version $JAVA_VERSION but at least 1.8 is required"
      exit 1
    fi
  else
    ADD_OPENS="--add-opens java.base/java.util=ALL-UNNAMED"
    if [[ "$JAVA_OPTS" ]]; then
      JAVA_OPTS="$JAVA_OPTS $ADD_OPENS"
    else
      JAVA_OPTS=$ADD_OPENS
    fi
  fi
fi

command -v realpath &>/dev/null || realpath() {
  SCRIPT_PATH=$([[ $1 == /* ]] && echo "$1" || echo "$PWD/${1#./}")
  if [[ -L "$SCRIPT_PATH" ]]; then
    readlink "$SCRIPT_PATH"
  else
    echo $SCRIPT_PATH
  fi
}

SCRIPT_DIR="$(dirname "$(realpath "$0")")"
KI_SHELL=$SCRIPT_DIR/../lib/ki-shell.jar

$_JAVA $JAVA_OPTS -jar "${KI_SHELL}" $@
