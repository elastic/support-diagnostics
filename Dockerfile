FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:caa7ac022c6c1715a69bd2693aaf7b8eddca372443df13dbf351f271bd1d2ad9 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:79407ed0315212612f32373f5afc0bec596fd92d2cf25ed4ee59435ba88147c4 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
