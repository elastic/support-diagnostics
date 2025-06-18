FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:eeb869768a4f5103c0a1680fbb4b9713d9e3bb5aa6918023961cdf5dfd621a3e AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:27e1f29bc141b01b1b37c4e9e54e03b0684284f14e02ea40b00aad83c66c1d45 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
