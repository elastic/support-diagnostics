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

if [ ! -r "/etc/elasticsearch" ]; then
    echo "Could not read config dir (/etc/elasticsearch).  Please verify permissions and rerun"
    exit 1
fi

if [ ! -r "/var/log/elasticsearch" ]; then
    echo "Could not read log dir (/var/log/elasticsearch).  Please verify permissions and rerun"
    exit 1
fi


[[ ${DIAG_DEBUG} != "" ]] && export DIAG_DEBUG_OPTS=" -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y"

[[ ${DIAG_JAVA_OPTS} == "" ]] && export DIAG_JAVA_OPTS="-Xms256m -Xmx2000m"

echo "Using ${DIAG_JAVA_OPTS} ${DIAG_DEBUG_OPTS} for options."
"$JAVA" $DIAG_JAVA_OPTS ${DIAG_DEBUG_OPTS} -cp .:./lib/*  com.elastic.support.diagnostics.DiagnosticApp "$@"
