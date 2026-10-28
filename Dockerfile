FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.5-r0-dev@sha256:2ccb896d7dcdbc10d8c79cb63d0862096fa627ec07b54ee2a5e07d524ae670d9 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.5-r0@sha256:0b95daa63fd741bd817633f36bca899aa81ed99083dc99c6ae128f90812e4198 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
