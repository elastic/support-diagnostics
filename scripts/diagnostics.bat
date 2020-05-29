@echo off
setlocal enabledelayedexpansion

SET scriptpath=%~dp0
SET diagpath=%scriptpath:~0,-1%
SET libpath=%diagpath%\lib\NUL

IF NOT EXIST %libpath% (
    ECHO "Runtimes library does not exist - make sure you are running the "
    ECHO "archive with 'dist' in the name, not the one labeled: 'source'."
    EXIT
)
set JAVA_EXEC=java
if not defined JAVA_HOME (
  set JAVA_EXEC=java
  echo No Java Home was found. Using current path. If execution fails please install Java and make sure it is in the search path or exposed via the JAVA_HOME environment variable.
) else (
  echo JAVA_HOME found, using !JAVA_HOME!
  set JAVA_EXEC=!JAVA_HOME!\bin\java
)

if not defined DIAG_JAVA_OPTIONS (
  set DIAG_JAVA_OPTIONS=-Xmx512m
)

"%JAVA_EXEC%" %DIAG_JAVA_OPTIONS% -cp %diagpath%\config;%diagpath%\lib\* com.elastic.support.diagnostics.DiagnosticApp %*

endlocal
