#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.4-M3
KI_VERSION=0.3.0-SNAPSHOT

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler/${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar

# KShell path

KSHELL=$HOME/.m2/repository/sparklin/kshell/${KI_VERSION}/kshell-${KI_VERSION}.jar

JARS=${KOTLIN_COMPILER}:${KOTLIN_STDLIB}:${KOTLIN_REFLECT}:${KOTLIN_SCRIPT_RUNTIME}

java -classpath ${JARS}:${KSHELL} org.jetbrains.kotlin.ki.shell.KotlinShell $@
