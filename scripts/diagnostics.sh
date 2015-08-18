#!/usr/bin/env bash
echo "Using Java Home: ${JAVA_HOME}"
[[ ${DIAG_JAVA_OPTS} == "" ]] && export DIAG_JAVA_OPTS="-Xmx512m"
echo "Using ${DIAG_JAVA_OPTS} for options."
$JAVA_HOME/bin/java $DIAG_JAVA_OPTS -cp .:support-diagnostics.jar com.elastic.support.DiagnosticApp "$@"