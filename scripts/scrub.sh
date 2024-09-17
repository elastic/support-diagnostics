#!/usr/bin/env bash

scriptDir="$(cd -- "$(dirname -- "$0")" && pwd)"
libDir="$scriptDir"'/lib'

if [ ! -d "$libDir" ]; then
    echo "Diagnostic executable not found:"
    echo ""
    echo "Please make sure that you are running with the archive ending with"
    echo "'-dist.zip' in the name and not the one labeled 'Source code'."
    echo ""
    echo "Download at https://github.com/elastic/support-diagnostics/releases/latest"
    exit 4
fi

if [ -x "${JAVA_HOME}/bin/java" ]; then
    JAVA="${JAVA_HOME}/bin/java"
else
    JAVA=`which java`
fi

echo "Using ${JAVA} as Java Runtime"

if [ ! -x "$JAVA" ]; then
    echo "Could not find any executable java binary. Please install java in your PATH and/or set JAVA_HOME"
    exit 1
fi

[ "${DIAG_DEBUG}" != "" ] && export DIAG_DEBUG_OPTS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y"

[ "${DIAG_JAVA_OPTS}" = "" ] && export DIAG_JAVA_OPTS="-Xms8g -Xmx8g"

echo "Using ${DIAG_JAVA_OPTS} ${DIAG_DEBUG_OPTS} for options."
"$JAVA" ${DIAG_JAVA_OPTS} ${DIAG_DEBUG_OPTS} -cp "${scriptDir}/config:${scriptDir}/lib/*" co.elastic.support.scrub.ScrubApp "$@"
