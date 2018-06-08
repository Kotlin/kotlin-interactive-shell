#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.2.41
KSHELL_REPL_API_VERSION=0.2.2-1.2.41
SPARKLIN_VERSION=0.2-SNAPSHOT

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler/${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar
KSHELL_CONSOLE_JLINE3=$HOME/.m2/repository/sparklin/jline3-shaded/${SPARKLIN_VERSION}/jline3-shaded-${SPARKLIN_VERSION}.jar
KSHELL_REPL_API=$HOME/.m2/repository/com/github/khud/kshell-repl-api/${KSHELL_REPL_API_VERSION}/kshell-repl-api-${KSHELL_REPL_API_VERSION}.jar

# KShell path
KSHELL=$HOME/.m2/repository/sparklin/kshell/${SPARKLIN_VERSION}/kshell-${SPARKLIN_VERSION}.jar

JARS=${KOTLIN_COMPILER}:${KOTLIN_STDLIB}:${KOTLIN_REFLECT}:${KOTLIN_SCRIPT_RUNTIME}:${KSHELL_CONSOLE_JLINE3}:${KSHELL_REPL_API}

java -classpath ${JARS}:${KSHELL} com.github.khud.sparklin.kshell.KotlinShell $@
