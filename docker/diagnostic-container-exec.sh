#!/usr/bin/env bash

#
# Sample script to set up an interactive session in the Docker diagnostic image created via the
# docker-build.sh script. The -v volume setting sends the output from the diagnostic to
# a directory named docker-diagnostic-output in the home directory of the user running
# the script. Simply change the directory location to the left of the colon if you wish to
# write to a different location. Be sure that this target folder has sufficient permissions
# to create the output files.
#
docker run -it -v ~/docker-diagnostic-output:/diagnostic-output  support-diagnostics-app  bash
