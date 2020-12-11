@echo off
set KI_VERSION=0.3.0-SNAPSHOT

set SCRIPT_PATH=%~dp0
set KI_SHELL=%SCRIPT_PATH%\..\lib\ki-shell-%KI_VERSION%.jar

java -jar %KI_SHELL% %*
