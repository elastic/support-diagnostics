@echo off
setlocal enabledelayedexpansion

SET scriptpath=%~dp0
SET diagpath=%scriptpath:~0,-1%
SET libpath=%diagpath%\lib\NUL

IF NOT EXIST %libpath% (
    ECHO Diagnostic executable not found:
    ECHO.
    ECHO Please make sure that you are running with the archive ending with
    ECHO '-dist.zip' in the name and not the one labeled 'Source code'.
    ECHO.
    ECHO Download at https://github.com/elastic/support-diagnostics/releases/latest
    EXIT /b 400
)

set JAVA_EXEC=java
if not defined JAVA_HOME (
  set JAVA_EXEC=java
  echo No Java Home was found. Using current path. If execution fails please install Java and make sure it is in the search path or exposed via the JAVA_HOME environment variable.
) else (
  echo JAVA_HOME found, using !JAVA_HOME!
  set JAVA_EXEC=!JAVA_HOME!\bin\java
)

if defined DIAG_DEBUG (
  set DIAG_DEBUG_OPTS=-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y
)

if not defined DIAG_JAVA_OPTS (
  set DIAG_JAVA_OPTS=-Xms2g -Xmx2g
)

echo Using %DIAG_JAVA_OPTS% %DIAG_DEBUG_OPTS% for options.
"%JAVA_EXEC%" %DIAG_JAVA_OPTS% %DIAG_DEBUG_OPTS% -cp %diagpath%\config;%diagpath%\lib\* co.elastic.support.monitoring.MonitoringImportApp %*

endlocal
