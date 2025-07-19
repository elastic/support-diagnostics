FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:bdf2609159a6683dec2f00ec4657fd601d5bf47bc2e63cd84ba7c449ab84aea2 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:13f8372ec3ce50584c0272047c3bf1fb2dba1d429178dcc7b4eec11b307f1b79 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
