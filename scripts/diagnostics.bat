@echo off
setlocal

if not defined JAVA_HOME (
  echo No JAVA_HOME defined - please set to the location of your Java installation.
  goto EOF
)

if not defined DIAG_JAVA_OPTIONS ( 
  set DIAG_JAVA_OPTIONS=-Xmx512m 
)

%JAVA_EXEC% %DIAG)JAVA_OPTIONS% -cp .\;support-diagnostics.jar com.elastic.support.DiagnosticApp %*

:EOF

endlocal