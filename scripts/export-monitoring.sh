#!/usr/bin/env bash

scriptDir="$0"
scriptDir="${scriptDir/\/export-monitoring.sh/$''}"
libDir="$scriptDir"'/lib'

if [ ! -d "$libDir" ]; then
    echo "Diagnostic executable not found:"
    echo ""
    echo "Please make sure that you are running with the archive ending with"
    echo "'-dist.zip' in the name and not the one labeled 'Source code'."
    echo ""
    echo "Download at https://github.com/elastic/support-diagnostics/releases/latest"
    exit 400
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

[[ "${DIAG_DEBUG}" != "" ]] && export DIAG_DEBUG_OPTS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y"

[[ "${DIAG_JAVA_OPTS}" == "" ]] && export DIAG_JAVA_OPTS="-Xms256m -Xmx2000m"

echo "Using ${DIAG_JAVA_OPTS} ${DIAG_DEBUG_OPTS} for options."
"$JAVA" $DIAG_JAVA_OPTS ${DIAG_DEBUG_OPTS} -cp "${scriptDir}/config:${scriptDir}/lib/*" co.elastic.support.monitoring.MonitoringExportApp "$@"
