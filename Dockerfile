FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:c7d00d37d776a767031449092c1f90d3a497bc230e5d3fe8f4465c938dad011e AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:167c184a24897f6b0b4dd3c8e9a54a6706148dd503295e3d47d15c0fbc50fb63 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
