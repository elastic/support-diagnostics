FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:79038114e2a9eac1d83d53275b46afdaf29cb9a7e9b1b8e84d00ea7154be7fd7 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:fe0bcd211b53610fb41b6de121f4b88982798d38715c0c2b8ec04e6f64e2fe53 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
