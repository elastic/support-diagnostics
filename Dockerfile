FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:ef248e13a42c9e87e841befc83b21c2e79e94e5956fbb9312c7999a8bea5be28 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:a2a9109b427a602d67a9a658ba7aaf21fd139e198a308e88e117ccc1d4e8b74c AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
