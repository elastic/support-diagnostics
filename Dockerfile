FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r5-dev@sha256:0683788c08680680a04322dec0d76e58cec03a7d4ddb9fb098f9e09ce868ddc4 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r5@sha256:2f7fb6653db0a8c2c4c74db1257082155a665cf27bbee3dfbfa55db92d7d6ff2 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
