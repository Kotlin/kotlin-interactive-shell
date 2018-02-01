#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.2.20
SPARKLIN_VERSION=0.1

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler/${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar
KSHELL_CONSOLE_JLINE2=$HOME/.m2/repository/sparklin/kshell-console-jline2/SNAPSHOT/kshell-console-jline2-SNAPSHOT.jar

# Spark 1.x support
SPARKLIN_PLUGIN=$HOME/.m2/repository/sparklin/sparklin-spark-1.x/SNAPSHOT/sparklin-spark-1.x-SNAPSHOT.jar

# Please specify path to KShell configuration file, note that:
# 1) If property config.path isn't specified Sparklin is trying find config file in path $HOME/.kshell
# 2) If file does not exist Spark support would not work because the plugin is disabled by default
SPARKLIN_CONFIG=$HOME/Documents/research_projects/sparklin/conf/spark1x.properties

# Apache Spark home directory must point to Spark version 1.x > 1.6
SPARK_HOME=$HOME/Programs/spark-1.6.1-bin-hadoop2.6

JARS=${KOTLIN_COMPILER},${KOTLIN_STDLIB},${KOTLIN_REFLECT},${KOTLIN_SCRIPT_RUNTIME},${KSHELL_CONSOLE_JLINE2},${SPARKLIN_PLUGIN}

${SPARK_HOME}/bin/spark-submit --jars=${JARS} --conf "spark.driver.extraJavaOptions=-Dconfig.path=${SPARKLIN_CONFIG}" --master "local[2]" --class "kshell.KotlinShell" \
		kshell/target/kshell-SNAPSHOT.jar \
		$@