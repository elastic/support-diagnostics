#!/bin/bash
#
# Pushes Support Diagnostic images to hosted Docker.
#
# Same way as Elasticsearch: build linux/amd64 and linux/arm64 and
# combine them with a Docker manifest (buildx --platform + --push).
# https://github.com/elastic/elasticsearch/blob/main/distribution/docker/README.md
# (section "Multi-architecture images").
#
# Also tags :latest on that same manifest (ES unified release does the
# combine; we retag latest here because this script is the release path).
#
# Usage:
#   ./push-docker.sh           # version from gradle.properties
#   ./push-docker.sh 9.4.2
#

IMAGE="docker.elastic.co/support/diagnostics"

echo "$IMAGE"

if [[ $# -eq 1 ]]; then
  VERSION=${1}
else
  VERSION=$(grep '^version=' gradle.properties | cut -d'=' -f2)
fi

echo "Pushing $VERSION and latest (linux/amd64,linux/arm64)"

docker buildx create --use --name multiarch-builder --driver docker-container \
  || docker buildx use multiarch-builder

( set -x ; docker buildx build --platform linux/amd64,linux/arm64 --push \
  -f Dockerfile \
  -t "$IMAGE:$VERSION" \
  -t "$IMAGE:latest" \
  . )

echo "All done!"
