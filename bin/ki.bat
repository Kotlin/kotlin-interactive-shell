@echo off
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set KI_SHELL="%SCRIPT_DIR%\..\lib\ki-shell.jar"
set JAVA_OPTS=%*

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)

set JAVA_VERSION=%JAVA_VERSION:"=%

for /f "delims=. tokens=1-3" %%v in ("%JAVA_VERSION%") do (
    if %%v EQU 1 (
        if %%w LSS 8 (
	        @echo java has version !JAVA_VERSION! but at least 1.8 is required
	        exit /b 1
	    )
    ) else (
	    set ADD_OPENS=--add-opens java.base/java.util=ALL-UNNAMED
        if not defined JAVA_OPTS (
	        set JAVA_OPTS=!ADD_OPENS!
	    ) else (
            set JAVA_OPTS=!JAVA_OPTS! !ADD_OPENS!
        )
    )
    java !JAVA_OPTS! -jar !KI_SHELL!
)

endlocal