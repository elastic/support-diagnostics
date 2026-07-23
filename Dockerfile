FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.4-r0-dev@sha256:9185b799366f250efbab5f0698cdf7f0e4449024ab52eb53ff5fb1ef1a0a985a AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.4-r0@sha256:4c9c7594454c183fca1bbaf63be09f80e76112901347ed6c8fb48c4d037485f8 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
