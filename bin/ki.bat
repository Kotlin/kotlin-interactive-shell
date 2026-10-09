@echo off
setlocal enabledelayedexpansion

set "KI_SHELL=%~dp0..\lib\ki-shell.jar"

set "_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "_JAVA=%JAVA_HOME%\bin\java.exe"

set "JAVA_VERSION="
for /f "tokens=3" %%g in ('call "%_JAVA%" -version 2^>^&1 ^| findstr /i version') do set "JAVA_VERSION=%%~g"
if not defined JAVA_VERSION (
    echo java not found 1>&2
    exit /b 1
)

for /f "delims=.-+ tokens=1" %%v in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%v"
if !JAVA_MAJOR! LSS 17 (
    echo java has version !JAVA_VERSION! but at least 17 is required 1>&2
    exit /b 1
)

if not exist "!KI_SHELL!" (
    echo !KI_SHELL! not found, build it with 'mvn package' first 1>&2
    exit /b 1
)

set "JAVA_FLAGS=--add-opens java.base/java.util=ALL-UNNAMED"
rem The compiler uses sun.misc.Unsafe, which JDK 24+ warns about
if !JAVA_MAJOR! GEQ 24 set "JAVA_FLAGS=!JAVA_FLAGS! --sun-misc-unsafe-memory-access=allow"

"!_JAVA!" %JAVA_OPTS% !JAVA_FLAGS! -jar "!KI_SHELL!" %*
exit /b !ERRORLEVEL!
