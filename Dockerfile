FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1-dev@sha256:f2ea861e8cd805d2935d8a0a8d95953437c8c4263cbcb22721742859e6a95aae AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1@sha256:499702a56ed18c16797a16a40cdb4fa2a86f8dcf6739d069aa361962ef539a2a AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
