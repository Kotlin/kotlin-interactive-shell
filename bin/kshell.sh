#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.2.30
SPARKLIN_VERSION=0.1.3

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler/${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar
KSHELL_CONSOLE_JLINE2=$HOME/.m2/repository/sparklin/kshell-console-jline2/${SPARKLIN_VERSION}/kshell-console-jline2-${SPARKLIN_VERSION}.jar
KSHELL_REPL_API=$HOME/.m2/repository/sparklin/kshell-repl-api/${SPARKLIN_VERSION}/kshell-repl-api-${SPARKLIN_VERSION}.jar

# KShell path
KSHELL=$HOME/.m2/repository/sparklin/kshell/${SPARKLIN_VERSION}/kshell-${SPARKLIN_VERSION}.jar

JARS=${KOTLIN_COMPILER}:${KOTLIN_STDLIB}:${KOTLIN_REFLECT}:${KOTLIN_SCRIPT_RUNTIME}:${KSHELL_CONSOLE_JLINE2}:${KSHELL_REPL_API}

java -classpath ${JARS}:${KSHELL} sparklin.kshell.KotlinShell $@
