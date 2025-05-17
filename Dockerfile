FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:47abdd82ba7a667a559cb75109236028289d3507124621327e10f8bc045a6551 AS builder

#####################
# Install dev tools
#####################
USER root

# need to be root to be able to install maven
RUN apk add --no-cache maven

#####################
# Build code
#####################
WORKDIR /build

COPY ./ ./

RUN mvn package

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:05c66ba034d56f3e4cc5a7b7416de18eed1e7c063f5d1d19ff7df2feda090364 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
