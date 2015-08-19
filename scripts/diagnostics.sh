#!/usr/bin/env bash
: ${JAVA_HOME:?"A JAVA_HOME variable is required.  Please set it to point to your Java installation directory."}
echo "Using Java Home: ${JAVA_HOME}"

[[ ${DIAG_JAVA_OPTS} == "" ]] && export DIAG_JAVA_OPTS="-Xmx512m"
echo "Using ${DIAG_JAVA_OPTS} for options."
$JAVA_HOME/bin/java $DIAG_JAVA_OPTS -cp .:support-diagnostics.jar com.elastic.support.DiagnosticApp "$@"