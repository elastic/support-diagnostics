FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7-dev@sha256:b8ee23335afba5ca8f725b9ed026e85c1c5a172fde9454aade9118a665e73e50 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7@sha256:31c8851dd605d8211975a3f7d8bcdd98faa7bfa2628dbbb401b94ab6cd23b115 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
