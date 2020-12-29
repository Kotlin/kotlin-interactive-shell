@echo off

set SCRIPT_PATH=%~dp0
set KI_SHELL=%SCRIPT_PATH%\..\lib\ki-shell-*.jar

java -jar %KI_SHELL% %*
