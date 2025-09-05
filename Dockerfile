FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6-dev@sha256:50d774335f5b42d125c88f604a2bd40d0ce9c201d2f6905d425b00509826099a AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6@sha256:799e1387075a92f8491777856ce26c58eaae4da8df059e0a9180b8f240048497 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
