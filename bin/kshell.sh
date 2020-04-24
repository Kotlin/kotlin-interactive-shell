#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.3.31
KSHELL_REPL_API_VERSION=0.2.7-1.3.31
SPARKLIN_VERSION=0.2.7-SNAPSHOT
TROVE4J=$HOME/.m2/repository/org/jetbrains/intellij/deps/trove4j/1.0.20181211/trove4j-1.0.20181211.jar
ANNOTATIONS=$HOME/.m2/repository/org/jetbrains/annotations/17.0.0/annotations-17.0.0.jar

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler/${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar
KSHELL_REPL_API=$HOME/.m2/repository/com/github/khud/kshell-repl-api/${KSHELL_REPL_API_VERSION}/kshell-repl-api-${KSHELL_REPL_API_VERSION}.jar

# KShell path

KSHELL=$HOME/.m2/repository/sparklin/kshell/${SPARKLIN_VERSION}/kshell-${SPARKLIN_VERSION}.jar

JARS=${KOTLIN_COMPILER}:${KOTLIN_STDLIB}:${KOTLIN_REFLECT}:${KOTLIN_SCRIPT_RUNTIME}:${KSHELL_REPL_API}:${TROVE4J}:${ANNOTATIONS}

java -classpath ${JARS}:${KSHELL} com.github.khud.sparklin.kshell.KotlinShell $@
