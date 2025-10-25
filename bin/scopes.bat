@echo off
REM Scopes CLI wrapper script for Windows (CMD/Batch)
REM This script provides a convenient way to run the Scopes JAR file

setlocal enabledelayedexpansion

REM Determine the directory where this script is located
set "SCRIPT_DIR=%~dp0"

REM Try multiple JAR file locations
set "JAR_FILE="
for %%L in (
    "%SCRIPT_DIR%scopes.jar"
    "%SCRIPT_DIR%..\lib\scopes.jar"
    "%SCRIPT_DIR%..\share\scopes\scopes.jar"
) do (
    if exist %%L (
        set "JAR_FILE=%%~L"
        goto :jar_found
    )
)

:jar_not_found
echo Error: scopes.jar not found in any of the expected locations: 1>&2
echo   - %SCRIPT_DIR%scopes.jar 1>&2
echo   - %SCRIPT_DIR%..\lib\scopes.jar 1>&2
echo   - %SCRIPT_DIR%..\share\scopes\scopes.jar 1>&2
echo. 1>&2
echo Please ensure Scopes is installed correctly. 1>&2
exit /b 1

:jar_found
REM Check if Java is available
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Error: Java is not installed or not in PATH 1>&2
    echo. 1>&2
    echo Scopes requires Java 21 or later to run. 1>&2
    echo. 1>&2
    echo Installation instructions: 1>&2
    echo   Download from: https://adoptium.net/ 1>&2
    echo   Or use Chocolatey: choco install openjdk21 1>&2
    echo   Or use Scoop: scoop install openjdk21 1>&2
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%v"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)

REM Extract major version (handle both old format like 1.8 and new format like 21.0.1)
if defined JAVA_VERSION (
    for /f "tokens=1 delims=." %%m in ("!JAVA_VERSION!") do set "MAJOR_VERSION=%%m"
    if "!MAJOR_VERSION!"=="1" (
        for /f "tokens=2 delims=." %%m in ("!JAVA_VERSION!") do set "MAJOR_VERSION=%%m"
    )
) else (
    echo Warning: Could not determine Java version, proceeding anyway... 1>&2
    goto :after_version_check
)

if defined MAJOR_VERSION if !MAJOR_VERSION! LSS 21 (
    echo Error: Java !MAJOR_VERSION! is installed, but Scopes requires Java 21 or later 1>&2
    echo. 1>&2
    echo Please upgrade your Java installation: 1>&2
    echo   Current version: Java !MAJOR_VERSION! 1>&2
    echo   Required version: Java 21+ 1>&2
    exit /b 1
)

:after_version_check

REM Load Java options from config file, then environment variables
REM Priority: SCOPES_JAVA_OPTS > JAVA_OPTS > config file
set "JAVA_OPTIONS="

REM Check for config file in standard locations
set "CONFIG_FILE="
if exist "%APPDATA%\scopes\scopes.conf" (
    set "CONFIG_FILE=%APPDATA%\scopes\scopes.conf"
) else if exist "%USERPROFILE%\.scopes\scopes.conf" (
    set "CONFIG_FILE=%USERPROFILE%\.scopes\scopes.conf"
)

if defined CONFIG_FILE (
    for /f "usebackq tokens=1,* delims==" %%a in ("%CONFIG_FILE%") do (
        REM Strip whitespace and quotes from key and value
        set "TEMP_KEY=%%a"
        set "TEMP_KEY=!TEMP_KEY: =!"
        if /i "!TEMP_KEY!"=="JAVA_OPTS" (
            set "TEMP_OPTS=%%b"
            REM Strip leading/trailing whitespace and quotes
            for /f "tokens=* delims= " %%x in ("!TEMP_OPTS!") do set "TEMP_OPTS=%%x"
            set "TEMP_OPTS=!TEMP_OPTS:"=!"
            set "JAVA_OPTIONS=!TEMP_OPTS!"
        )
    )
)

REM Override with environment variables
if defined JAVA_OPTS (
    set "JAVA_OPTIONS=%JAVA_OPTS%"
)
if defined SCOPES_JAVA_OPTS (
    set "JAVA_OPTIONS=%SCOPES_JAVA_OPTS%"
)

REM Execute the JAR file with all arguments passed through
if defined JAVA_OPTIONS (
    java %JAVA_OPTIONS% -jar "%JAR_FILE%" %*
) else (
    java -jar "%JAR_FILE%" %*
)
exit /b %ERRORLEVEL%
