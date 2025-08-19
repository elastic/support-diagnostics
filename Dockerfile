FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6-dev@sha256:d9719d0c36f9cf391842b9be8f9dae54c802f22778a8991df9d7cf92cc0fcb3a AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6@sha256:249fe13b9e5891c335eae654e19a1002e2b5cd67e760981be30565a4a077064c AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
