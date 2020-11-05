#!/usr/bin/env bash

KI_VERSION=0.3.0-SNAPSHOT

KI_SHELL=$HOME/.m2/repository/org/jetbrains/kotlin/ki-shell/${KI_VERSION}/ki-shell-${KI_VERSION}.jar

java -jar ${KI_SHELL} $@
