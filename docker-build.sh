#!/usr/bin/env bash
#
# Same platforms as Elasticsearch: linux/amd64 + linux/arm64, combined
# into one Docker manifest (buildx). See
# https://github.com/elastic/elasticsearch/blob/main/distribution/docker/README.md
# (section "Multi-architecture images").
#
# Multi-arch output stays in the builder. To publish, use ./push-docker.sh.
#

IMAGE="docker.elastic.co/support/diagnostics"

docker buildx create --use --name multiarch-builder --driver docker-container \
  || docker buildx use multiarch-builder

( set -x ; docker buildx build --platform linux/amd64,linux/arm64 \
  -f Dockerfile \
  -t "$IMAGE:latest" \
  . )
