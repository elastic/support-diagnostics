FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:c999bd927c1bd1a406fa6220811bb4e6a268f685662133e410c1a0f8c5592478 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:5c24dc6beba16f7e45010d11de9b70aa435778b8cec1610b89f00cb9c906a069 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
