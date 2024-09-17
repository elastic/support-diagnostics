#!/bin/bash

#
# Pushes Support Diagnostic images to hosted Docker.
#
# 1) Pushes the image tagged `latest` (which should have been built via
# `$ docker compose build`).
# 2) If a version was passed in:
# 2a) Add that version as a tag to latest.
# 2b) Push that newly tagged version to hosted Docker.
#
# Example usages:
# 1) Push latest
# $ ./push-docker.sh
# 2) Push latest and also tag as 9.1.1
# $ ./push-docker.sh 9.1.1
#

IMAGE="docker.elastic.co/support/diagnostics"

echo "$IMAGE"

echo "Pushing latest"

# NOTE: The pattern with ( set -x ; command ) makes it so that the command runs
# in a sub-shell where the command is echo'd to the screen when run. We don't
# want that mode permanently because the echo lines will duplicate in the console.
( set -x ; docker push "$IMAGE:latest" )

# If there is a version parameter passed in
if [[ $# -eq 1 ]]; then
  VERSION=${1}

  echo "Tagging version $VERSION"
  ( set -x ; docker tag "$IMAGE:latest" "$IMAGE:$VERSION" )

  echo "Pushing version $VERSION"
  ( set -x ; docker push "$IMAGE:$VERSION" )
fi

echo "All done!"
