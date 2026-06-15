FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r3-dev@sha256:c27921b4c9214d11de99d7371a857863faf431b16802dbffe087c74f49d74c3e AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r3@sha256:e12de449be08becc5fc2b26050e861cfe2af0257708b9819c0e1016fe04c49a7 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
