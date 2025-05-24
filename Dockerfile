FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:ca5a366c6af3916e21f4d53b6985fad0bf20b9c156f56b8fbf12f003febfd1e6 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:12f8bc59d431f2dbdd412b90bd39d3538de61cbf22d9dad33d4db5e7d1d57e21 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
