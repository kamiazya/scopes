@echo off
rem ##############################################################################
rem
rem   Scopes CLI start up script for Windows
rem
rem   This script provides a Gradle wrapper-style development experience for
rem   the Scopes CLI, making it easier to debug and test during development.
rem
rem ##############################################################################

setlocal enabledelayedexpansion

rem Set APP_HOME to the directory containing this script
set APP_HOME=%~dp0
if "%APP_HOME%" == "" set APP_HOME=.
set APP_HOME=%APP_HOME:~0,-1%

rem Default configuration
set DEFAULT_LOG_LEVEL=INFO
set DEFAULT_TRANSPORT=local
set DEFAULT_JVM_OPTS=

rem Load wrapper configuration if it exists
set WRAPPER_CONFIG=%APP_HOME%\.scopes\wrapper.properties
if exist "%WRAPPER_CONFIG%" (
    for /f "tokens=1,2 delims==" %%a in (%WRAPPER_CONFIG%) do (
        if "%%a"=="scopes.log.level" set DEFAULT_LOG_LEVEL=%%b
        if "%%a"=="scopes.transport" set DEFAULT_TRANSPORT=%%b
        if "%%a"=="scopes.jvm.opts" set DEFAULT_JVM_OPTS=%%b
    )
)

rem Initialize variables
set DEBUG_MODE=false
set PROFILE_MODE=false
set JVM_DEBUG_PORT=5005
set JVM_OPTS=%DEFAULT_JVM_OPTS%
set LOG_LEVEL=%DEFAULT_LOG_LEVEL%
set TRANSPORT=%DEFAULT_TRANSPORT%
set GRADLE_TASK=run
set REMAINING_ARGS=

:parse_args
if "%~1"=="" goto end_parse
if "%~1"=="--debug" (
    set DEBUG_MODE=true
    shift
    goto parse_args
)
if "%~1"=="--debug-port" (
    set JVM_DEBUG_PORT=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--profile" (
    set PROFILE_MODE=true
    shift
    goto parse_args
)
if "%~1"=="--log-level" (
    set LOG_LEVEL=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--transport" (
    set TRANSPORT=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--gradle-task" (
    set GRADLE_TASK=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--jvm-opts" (
    set JVM_OPTS=%JVM_OPTS% %~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--help-wrapper" (
    goto show_help
)
set REMAINING_ARGS=%REMAINING_ARGS% %~1
shift
goto parse_args

:end_parse

rem Find java command
set JAVACMD=java
if defined JAVA_HOME (
    set JAVACMD=%JAVA_HOME%\bin\java.exe
    if not exist "!JAVACMD!" (
        echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
        echo.
        echo Please set the JAVA_HOME variable in your environment to match the
        echo location of your Java installation.
        exit /b 1
    )
)

rem Build JVM arguments
if "%DEBUG_MODE%"=="true" (
    set JVM_OPTS=%JVM_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:%JVM_DEBUG_PORT%
    echo 🐛 Debug mode enabled on port %JVM_DEBUG_PORT%
)

if "%PROFILE_MODE%"=="true" (
    set JVM_OPTS=%JVM_OPTS% -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=scopes-profile.jfr
    echo 📊 Profiling enabled (output: scopes-profile.jfr)
)

rem Set environment variables for the CLI
set SCOPES_LOG_LEVEL=%LOG_LEVEL%
set SCOPES_TRANSPORT=%TRANSPORT%

rem Build Gradle arguments
set GRADLE_ARGS=%GRADLE_TASK%
if "%GRADLE_TASK%"=="run" (
    set GRADLE_ARGS=:apps-scopes:run
    if not "%REMAINING_ARGS%"=="" (
        set GRADLE_ARGS=%GRADLE_ARGS% --args="%REMAINING_ARGS%"
    )
)

rem Set GRADLE_OPTS for JVM options
set GRADLE_OPTS=%JVM_OPTS%

echo 🚀 Starting Scopes CLI...
echo    Log Level: %LOG_LEVEL%
echo    Transport: %TRANSPORT%
if "%DEBUG_MODE%"=="true" (
    echo    JVM Options: %JVM_OPTS%
)
if not "%REMAINING_ARGS%"=="" (
    echo    CLI Args: %REMAINING_ARGS%
)
echo.

rem Execute Gradle with the CLI
"%APP_HOME%\gradlew.bat" %GRADLE_ARGS%
goto end

:show_help
echo Scopes CLI Development Wrapper
echo.
echo USAGE:
echo     scopes.bat [WRAPPER_OPTIONS] [CLI_ARGUMENTS...]
echo.
echo WRAPPER OPTIONS:
echo     --debug                 Enable JVM debug mode (port %JVM_DEBUG_PORT%)
echo     --debug-port PORT       Set JVM debug port (default: %JVM_DEBUG_PORT%)
echo     --profile               Enable JVM profiling
echo     --log-level LEVEL       Set log level (TRACE^|DEBUG^|INFO^|WARN^|ERROR)
echo     --transport TYPE        Set transport (local^|grpc)
echo     --gradle-task TASK      Set Gradle task (default: run)
echo     --jvm-opts OPTS         Additional JVM options
echo     --help-wrapper          Show this wrapper help
echo.
echo CLI ARGUMENTS:
echo     All remaining arguments are passed to the Scopes CLI application.
echo     Use 'scopes.bat help' to see CLI-specific help.
echo.
echo EXAMPLES:
echo     scopes.bat --debug create "My Task"
echo     scopes.bat --log-level DEBUG list
echo     scopes.bat --transport grpc --debug info
echo     scopes.bat --profile --gradle-task build
echo.
echo CONFIGURATION:
echo     Create %APP_HOME%\.scopes\wrapper.properties to set defaults:
echo         scopes.log.level=DEBUG
echo         scopes.transport=grpc
echo         scopes.jvm.opts=-Xmx1g

:end
endlocal