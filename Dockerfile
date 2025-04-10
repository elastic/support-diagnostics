FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3-dev@sha256:cc215a5a9f4897d909dcd70782b00d4fc075b38f5c3de0cc9f188ba0cf073b47 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3@sha256:a57e58f972e485397f583a58064cfa2f3d72ddddc85b73da72c9b7970840838e AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
