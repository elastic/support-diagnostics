@echo off
setlocal

set JAVA_EXEC=java
if not defined JAVA_HOME ( 
  set JAVA_EXEC=java
  echo No Java Home
) else (
  echo JAVA_HOME found, using %JAVA_HOME%
  set JAVA_EXEC=%JAVA_HOME%\bin\java
)

if not defined DIAG_JAVA_OPTIONS ( 
  set DIAG_JAVA_OPTIONS=-Xmx512m 
)

%JAVA_EXEC% %DIAG)JAVA_OPTIONS% -cp .\;support-diagnostics.jar com.elastic.support.DiagnosticApp %*

endlocal