FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:f0ed618479972fe1e362ee5bc0f06242a2a44d3422d0434d2db60023559c615c AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:8b6848a8fcbc6949d5cc8d36e16a1032e0bbb7a3c6743c1799512362499e6360 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
