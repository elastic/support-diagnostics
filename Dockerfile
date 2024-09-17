FROM docker.elastic.co/wolfi/jdk:openjdk-21.35-r1-dev@sha256:d7ca36452a68f28e4c4683062241e817b548844820a0ffd087451214e61eb188 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-21.35-r1@sha256:56d69799450d02a898e3e6c3ffd2cd5f34931788abdf6a102d07035bcae0d3d3 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
