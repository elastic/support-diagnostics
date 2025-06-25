FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4-dev@sha256:72f7676772b2028d301738955d4ac0096a2d9dd8dde8b4a8e6e4796a2b8c9429 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r4@sha256:2ede42a7210ec20b754ece42d46d9506d3e9c3ea4477b92b34ed27240c0a41ce AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
