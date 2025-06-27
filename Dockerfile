FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:b624b4b6722836b81b366bfae3e4c25d2dd93b4075933c9f8a718a8f6e564acc AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:89c1f9f12c052daeb42c6f926eee98353eb0bff4adc4a8c8eb99dec36e141e4d AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
