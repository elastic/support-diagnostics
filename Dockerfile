FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r2-dev@sha256:05e5fd81a2a335ca2872ee70dcd1cc6b183c412b0e559e5eca648a82872d281d AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r2@sha256:233d3c800cf649986ed5f0f947a11ce1c69476fab434909d50f3aefadfc35609 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
