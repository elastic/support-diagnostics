FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r5-dev@sha256:268b4992df679d107723332598d52866cfd4051b19c971fcb021571b316efc7b AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r5@sha256:aa2c60407ae05be9ccae1413298e284063ee140459d53e78c86f3c9b93eecef3 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
