FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:c1b455de61e3c0f6f3d3b8a88f04122b6da7e55bbc4f5876e7a5259743478d98 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:c1eaca8278307009d2fab4caece5fd9d3c377fbc71c69c85ac1b69a0f9d5fdcc AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
