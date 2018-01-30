#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=SNAPSHOT
KOTLIN_STDLIB=$MAVEN_REPO_BASE/kotlin-stdlib/$KOTLIN_VERSION/kotlin-stdlib-$KOTLIN_VERSION.jar
KOTLIN_COMPILER=$MAVEN_REPO_BASE/kotlin-compiler/$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.jar
KOTLIN_SCRIPT_RUNTIME=$MAVEN_REPO_BASE/kotlin-script-runtime/$KOTLIN_VERSION/kotlin-script-runtime-$KOTLIN_VERSION.jar
KOTLIN_REFLECT=$MAVEN_REPO_BASE/kotlin-reflect/$KOTLIN_VERSION/kotlin-reflect-$KOTLIN_VERSION.jar
KOTLIN_IDE_COMMON=$MAVEN_REPO_BASE/kotlin-ide-common/SNAPSHOT/kotlin-ide-common-SNAPSHOT.jar
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar

# FIXME: check paths before run
KSHELL_CONSOLE_JLINE2=$HOME/.m2/repository/sparklin/kshell-console-jline2/SNAPSHOT/kshell-console-jline2-SNAPSHOT.jar

JARS=$KOTLIN_COMPILER:$KOTLIN_STDLIB:$KOTLIN_REFLECT:$KOTLIN_SCRIPT_RUNTIME:$KOTLIN_IDE_COMMON:$KSHELL_CONSOLE_JLINE2
echo $JARS
java -classpath $JARS:kshell/target/kshell-SNAPSHOT.jar kshell.KotlinShell $@