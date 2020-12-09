@echo off
set KI_VERSION=0.3.0

set KI_SHELL=%USERPROFILE%\.m2\repository\org\jetbrains\kotlin\ki-shell\%KI_VERSION%\ki-shell-%KI_VERSION%.jar

java -jar %KI_SHELL% %*
