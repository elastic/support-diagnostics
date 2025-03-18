FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2-dev@sha256:a02c80c8afc415f8e7aabd3ff4daede2d9a8cb66ed41b8bd07bd7a3991dc2b35 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2@sha256:fd9fa62a2e41541704c42b0ca8f9e9b2b76945925d2dcb0144e6b29d560e014b AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
