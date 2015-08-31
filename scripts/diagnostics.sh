#!/usr/bin/env bash

if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=`which java`
fi

echo "Using ${JAVA} as Java Runtime"

if [ ! -x "$JAVA" ]; then
    echo "Could not find any executable java binary. Please install java in your PATH and/or set JAVA_HOME"
    exit 1
fi

[[ ${DIAG_JAVA_OPTS} == "" ]] && export DIAG_JAVA_OPTS="-Xmx512m"
echo "Using ${DIAG_JAVA_OPTS} for options."
JAVA $DIAG_JAVA_OPTS -cp .:support-diagnostics.jar com.elastic.support.DiagnosticApp "$@"