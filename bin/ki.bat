@echo off
set MAVEN_REPO_BASE=%USERPROFILE%\.m2\repository\org\jetbrains\kotlin
set KOTLIN_VERSION=1.4-M3
set KI_VERSION=0.3.0

rem Kotlin libraries
set KOTLIN_STDLIB=%MAVEN_REPO_BASE%\kotlin-stdlib\%KOTLIN_VERSION%\kotlin-stdlib-%KOTLIN_VERSION%.jar
set KOTLIN_COMPILER=%MAVEN_REPO_BASE%\kotlin-compiler\%KOTLIN_VERSION%\kotlin-compiler-%KOTLIN_VERSION%.jar
set KOTLIN_SCRIPT_RUNTIME=%MAVEN_REPO_BASE%\kotlin-script-runtime\%KOTLIN_VERSION%\kotlin-script-runtime-%KOTLIN_VERSION%.jar
set KOTLIN_REFLECT=%MAVEN_REPO_BASE%\kotlin-reflect\%KOTLIN_VERSION%\kotlin-reflect-%KOTLIN_VERSION%.jar

rem JLine2 console support
set FUSE_JANSI=%USERPROFILE%\.m2\repository\org\fusesource\jansi\jansi\1.18\jansi-1.18.jar

rem KShell path
set KSHELL=%USERPROFILE%\.m2\repository\sparklin\kshell\%KI_VERSION%\kshell-%KI_VERSION%.jar

set JARS=%KOTLIN_COMPILER%;%KOTLIN_STDLIB%;%KOTLIN_REFLECT%;%KOTLIN_SCRIPT_RUNTIME%

java -classpath %JARS%;%KSHELL%; org.jetbrains.kotlin.ki.shell.KotlinShell %*
