@echo off
set MAVEN_REPO_BASE=%USERPROFILE%\.m2\repository\org\jetbrains\kotlin
set KOTLIN_VERSION=1.2.61
set KSHELL_REPL_API_VERSION=0.2.4-1.2.60
set SPARKLIN_VERSION=0.2.4

rem Kotlin libraries
set KOTLIN_STDLIB=%MAVEN_REPO_BASE%\kotlin-stdlib\%KOTLIN_VERSION%\kotlin-stdlib-%KOTLIN_VERSION%.jar
set KOTLIN_COMPILER=%MAVEN_REPO_BASE%\kotlin-compiler\%KOTLIN_VERSION%\kotlin-compiler-%KOTLIN_VERSION%.jar
set KOTLIN_SCRIPT_RUNTIME=%MAVEN_REPO_BASE%\kotlin-script-runtime\%KOTLIN_VERSION%\kotlin-script-runtime-%KOTLIN_VERSION%.jar
set KOTLIN_REFLECT=%MAVEN_REPO_BASE%\kotlin-reflect\%KOTLIN_VERSION%\kotlin-reflect-%KOTLIN_VERSION%.jar

rem JLine2 console support
set FUSE_JANSI=%USERPROFILE%\.m2\repository\org\fusesource\jansi\jansi\1.15\jansi-1.15.jar
set KSHELL_REPL_API=%USERPROFILE%\.m2\repository\com\github\khud\kshell-repl-api\%KSHELL_REPL_API_VERSION%\kshell-repl-api-%KSHELL_REPL_API_VERSION%.jar

rem KShell path
set KSHELL=%USERPROFILE%\.m2\repository\sparklin\kshell\%SPARKLIN_VERSION%\kshell-%SPARKLIN_VERSION%.jar

set JARS=%KOTLIN_COMPILER%;%KOTLIN_STDLIB%;%KOTLIN_REFLECT%;%KOTLIN_SCRIPT_RUNTIME%;%KSHELL_REPL_API%

java -classpath %JARS%;%KSHELL%; com.github.khud.sparklin.kshell.KotlinShell %*
