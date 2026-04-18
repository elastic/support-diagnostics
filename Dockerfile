FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r5-dev@sha256:85965e6417dc7b79088725ed1405c010104d7e515c5af2d6b93c29e90b52e9a8 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r5@sha256:d2a6a7ea5b5e3880970b69f7e50878c29849ccbe3ea0e8aa335d5b9c0d217b2a AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
