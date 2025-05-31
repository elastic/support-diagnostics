FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:ce56f09eaca0ab98d4a9aa5625f048d91e69a5955a42a7f21cc8af019ebe1423 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:cdea8b6002469f1f5275813cdfcc7cf8a05323dd77c4d8219acb7d43c12c51c0 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
