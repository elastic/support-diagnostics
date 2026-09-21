FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.4-r0-dev@sha256:8de4c9934f627a2df8781b7ec53230ad149c323f8093969f38005b4e48e79643 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.4-r0@sha256:d039b146a8b18fa4abfb9642681b7cda5a9c538dbb8474df7552d0b18a3f3ceb AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
