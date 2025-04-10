FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3-dev@sha256:dadd8c2542d8b7949e838e0ecc419c9ef694700e7a9d17b95464bdb6442efaea AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3@sha256:0c7caeb0cd7fa8ca905fc766f8693cda23394b9deafae1225cf1903a97001275 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
