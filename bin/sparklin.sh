#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.2.20
KOTLIN_STDLIB=$MAVEN_REPO_BASE/kotlin-stdlib/$KOTLIN_VERSION/kotlin-stdlib-$KOTLIN_VERSION.jar
KOTLIN_COMPILER=$MAVEN_REPO_BASE/kotlin-compiler/$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.jar
KOTLIN_SCRIPT_RUNTIME=$MAVEN_REPO_BASE/kotlin-script-runtime/$KOTLIN_VERSION/kotlin-script-runtime-$KOTLIN_VERSION.jar
KOTLIN_REFLECT=$MAVEN_REPO_BASE/kotlin-reflect/$KOTLIN_VERSION/kotlin-reflect-$KOTLIN_VERSION.jar
KOTLIN_IDE_COMMON=$MAVEN_REPO_BASE/kotlin-ide-common/SNAPSHOT/kotlin-ide-common-SNAPSHOT.jar
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar

# FIXME: check paths before run
KSHELL_CONSOLE_JLINE2=$HOME/.m2/repository/sparklin/kshell-console-jline2/SNAPSHOT/kshell-console-jline2-SNAPSHOT.jar
SPARKLIN_PLUGIN=$HOME/.m2/repository/sparklin/sparklin-spark-1.x/SNAPSHOT/sparklin-spark-1.x-SNAPSHOT.jar
SPARKLIN_HOME=$HOME/Documents/research_projects/sparklin

JARS=$KOTLIN_COMPILER,$KOTLIN_STDLIB,$KOTLIN_REFLECT,$KOTLIN_SCRIPT_RUNTIME,$KOTLIN_IDE_COMMON,$KSHELL_CONSOLE_JLINE2,$SPARKLIN_PLUGIN

echo $JARS
spark-submit --jars=$JARS --conf "spark.driver.extraJavaOptions=-Dconfig.path=${SPARKLIN_HOME}/conf/sparklin.properties" --master "local[2]" --class "kshell.KotlinShell" \
		kshell/target/kshell-SNAPSHOT.jar \
		$@
#java -classpath $JARS:kshell/target/kshell-SNAPSHOT.jar -Dconfig.path=${SPARKLIN_HOME}/conf/sparklin.properties kshell.KotlinShell $@