#!/usr/bin/env bash

MAVEN_REPO_BASE=$HOME/.m2/repository/org/jetbrains/kotlin
KOTLIN_VERSION=1.4.0-dev-8551
SPARKLIN_VERSION=0.2.7-SNAPSHOT
SPARKLIN_HOME=$HOME/Work/ds/sparklin
TROVE4J=$HOME/.m2/repository/org/jetbrains/intellij/deps/trove4j/1.0.20190215/trove4j-1.0.20190215.jar
ANNOTATIONS=$HOME/.m2/repository/org/jetbrains/annotations/17.0.0/annotations-17.0.0.jar

# Kotlin libraries
KOTLIN_STDLIB=${MAVEN_REPO_BASE}/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_RUNTIME=${MAVEN_REPO_BASE}/kotlin-script-runtime/${KOTLIN_VERSION}/kotlin-script-runtime-${KOTLIN_VERSION}.jar
KOTLIN_REFLECT=${MAVEN_REPO_BASE}/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_COMMON=${MAVEN_REPO_BASE}/kotlin-scripting-common/${KOTLIN_VERSION}/kotlin-scripting-common-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_JVM=${MAVEN_REPO_BASE}/kotlin-scripting-jvm/${KOTLIN_VERSION}/kotlin-scripting-jvm-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPT_JVMHOST=${MAVEN_REPO_BASE}/kotlin-scripting-jvmhost-embeddable/${KOTLIN_VERSION}/kotlin-scripting-jvmhost-embeddable-${KOTLIN_VERSION}.jar
KOTLIN_COMPILER=${MAVEN_REPO_BASE}/kotlin-compiler-embeddabler/${KOTLIN_VERSION}/kotlin-compiler-embeddable-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPTING_COMPILER=${MAVEN_REPO_BASE}/kotlin-scripting-compiler-embeddabler/${KOTLIN_VERSION}/kotlin-scripting-compiler-embeddable-${KOTLIN_VERSION}.jar
KOTLIN_SCRIPTING_IDESERVICES=${MAVEN_REPO_BASE}/kotlin-scripting-ide-services-embeddabler/${KOTLIN_VERSION}/kotlin-scripting-ide-services-embeddable-${KOTLIN_VERSION}.jar

# JLine2 console support
FUSE_JANSI=$HOME/.m2/repository/org/fusesource/jansi/jansi/1.15/jansi-1.15.jar
KSHELL_REPL_API=$HOME/.m2/repository/com/github/khud/kshell-repl-api/${KSHELL_REPL_API_VERSION}/kshell-repl-api-${KSHELL_REPL_API_VERSION}.jar

# Spark 2.x support
SPARKLIN_BUNDLE=$HOME/.m2/repository/sparklin/sparklin-spark-2.x/${SPARKLIN_VERSION}/sparklin-spark-2.x-${SPARKLIN_VERSION}.jar
HDFS_BROWSER_PLUGIN=$HOME/.m2/repository/sparklin/hdfs-browser/${SPARKLIN_VERSION}/hdfs-browser-${SPARKLIN_VERSION}.jar

# Please specify path to KShell configuration file, note that:
# 1) If property config.path isn't specified Sparklin is trying find config file in path $HOME/.kshell
# 2) If file does not exist Spark support would not work because the plugin is disabled by default
export SPARKLIN_CONFIG=$SPARKLIN_HOME/conf/spark2x.properties

# KShell path
KSHELL=$HOME/.m2/repository/sparklin/kshell/${SPARKLIN_VERSION}/kshell-${SPARKLIN_VERSION}.jar

# Apache Spark home directory must point to Spark version 2.x
#SPARK_HOME=$HOME/Programs/spark-2.3.3-bin-hadoop2.7

JARS=${KOTLIN_COMPILER}${KOTLIN_SCRIPTING_COMPILER},,${KOTLIN_SCRIPTING_IDESERVICES},${KOTLIN_SCRIPT_JVM},${KOTLIN_SCRIPT_JVMHOST},${KOTLIN_SCRIPT_COMMON},${KOTLIN_STDLIB},${KOTLIN_REFLECT},${KOTLIN_SCRIPT_RUNTIME},${SPARKLIN_BUNDLE},${HDFS_BROWSER_PLUGIN},${TROVE4J},${ANNOTATIONS}

${SPARK_HOME}/bin/spark-submit --driver-class-path $JARS \
        --conf spark.ui.showConsoleProgress=true \
        --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file://$SPARKLIN_HOME/conf/log4j.properties" \
        --master "local[2]" --class "com.github.khud.sparklin.kshell.KotlinShell" \
		${SPARKLIN_BUNDLE} \
		$@