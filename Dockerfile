FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:55d83b0eced266ee0f28a973c4097f11baeba80836db0e6e3cdeafb08dc732bf AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:a7aaec7cd5fbda25d4e7163a825fcb374373d4fec9c1f4d14ed8c9e1a792b5de AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
