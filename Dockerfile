FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2-dev@sha256:aff6d34f02d03e04bea1b45b67172af74bb80cf5d71df2e3d133331e708b5914 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2@sha256:05820dd7e0218d8cf65b01297a2b71541404ae6585ff2cd91cdf8d0803db5225 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
