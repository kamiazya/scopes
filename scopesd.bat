@echo off
rem ##############################################################################
rem
rem   Scopes Daemon start up script for Windows
rem
rem   This script provides a Gradle wrapper-style development experience for
rem   the Scopes daemon, making it easier to debug and test during development.
rem
rem ##############################################################################

setlocal enabledelayedexpansion

rem Set APP_HOME to the directory containing this script
set APP_HOME=%~dp0
if "%APP_HOME%" == "" set APP_HOME=.
set APP_HOME=%APP_HOME:~0,-1%

rem Default configuration
set DEFAULT_LOG_LEVEL=INFO
set DEFAULT_HOST=127.0.0.1
set DEFAULT_PORT=0
set DEFAULT_JVM_OPTS=

rem Load wrapper configuration if it exists
set WRAPPER_CONFIG=%APP_HOME%\.scopes\wrapper.properties
if exist "%WRAPPER_CONFIG%" (
    for /f "tokens=1,2 delims==" %%a in (%WRAPPER_CONFIG%) do (
        if "%%a"=="scopesd.log.level" set DEFAULT_LOG_LEVEL=%%b
        if "%%a"=="scopesd.host" set DEFAULT_HOST=%%b
        if "%%a"=="scopesd.port" set DEFAULT_PORT=%%b
        if "%%a"=="scopesd.jvm.opts" set DEFAULT_JVM_OPTS=%%b
    )
)

rem Initialize variables
set DEBUG_MODE=false
set PROFILE_MODE=false
set JVM_DEBUG_PORT=5006
set JVM_OPTS=%DEFAULT_JVM_OPTS%
set LOG_LEVEL=%DEFAULT_LOG_LEVEL%
set HOST=%DEFAULT_HOST%
set PORT=%DEFAULT_PORT%
set GRADLE_TASK=run
set DAEMON_ARGS=

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
    shift
    goto parse_args
)
if "%~1"=="--log-level" (
    set LOG_LEVEL=%~2
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
if "%~1"=="--host" (
    set HOST=%~2
    set DAEMON_ARGS=%DAEMON_ARGS% %~1 %~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--port" (
    set PORT=%~2
    set DAEMON_ARGS=%DAEMON_ARGS% %~1 %~2
    shift
    shift
    goto parse_args
)
set DAEMON_ARGS=%DAEMON_ARGS% %~1
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
    set JVM_OPTS=%JVM_OPTS% -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=scopesd-profile.jfr
    echo 📊 Profiling enabled (output: scopesd-profile.jfr)
)

rem Set environment variables for the daemon
set SCOPES_LOG_LEVEL=%LOG_LEVEL%

rem Build Gradle arguments
set GRADLE_ARGS=%GRADLE_TASK%
if "%GRADLE_TASK%"=="run" (
    set GRADLE_ARGS=:apps-scopesd:run
    if not "%DAEMON_ARGS%"=="" (
        set GRADLE_ARGS=%GRADLE_ARGS% --args="%DAEMON_ARGS%"
    )
)

rem Set GRADLE_OPTS for JVM options
set GRADLE_OPTS=%JVM_OPTS%

echo 🚀 Starting Scopes Daemon...
echo    Log Level: %LOG_LEVEL%
echo    Host: %HOST%
echo    Port: %PORT%
if "%DEBUG_MODE%"=="true" (
    echo    JVM Options: %JVM_OPTS%
)
if not "%DAEMON_ARGS%"=="" (
    echo    Daemon Args: %DAEMON_ARGS%
)
echo.

rem Execute Gradle with the daemon
"%APP_HOME%\gradlew.bat" %GRADLE_ARGS%
goto end

:show_help
echo Scopes Daemon Development Wrapper
echo.
echo USAGE:
echo     scopesd.bat [WRAPPER_OPTIONS] [DAEMON_ARGUMENTS...]
echo.
echo WRAPPER OPTIONS:
echo     --debug                 Enable JVM debug mode (port %JVM_DEBUG_PORT%)
echo     --debug-port PORT       Set JVM debug port (default: %JVM_DEBUG_PORT%)
echo     --profile               Enable JVM profiling
echo     --log-level LEVEL       Set log level (TRACE^|DEBUG^|INFO^|WARN^|ERROR)
echo     --gradle-task TASK      Set Gradle task (default: run)
echo     --jvm-opts OPTS         Additional JVM options
echo     --help-wrapper          Show this wrapper help
echo.
echo DAEMON OPTIONS:
echo     --host HOST             Host to bind the gRPC server to (default: %DEFAULT_HOST%)
echo     --port PORT             Port to bind the gRPC server to (default: %DEFAULT_PORT%)
echo.
echo DAEMON ARGUMENTS:
echo     All daemon-specific arguments are passed to the Scopes daemon application.
echo.
echo EXAMPLES:
echo     scopesd.bat --debug
echo     scopesd.bat --log-level DEBUG --host 0.0.0.0 --port 50051
echo     scopesd.bat --profile --debug-port 5007
echo     scopesd.bat --gradle-task build
echo.
echo CONFIGURATION:
echo     Create %APP_HOME%\.scopes\wrapper.properties to set defaults:
echo         scopesd.log.level=DEBUG
echo         scopesd.host=0.0.0.0
echo         scopesd.port=50051
echo         scopesd.jvm.opts=-Xmx2g

:end
endlocal