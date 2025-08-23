FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6-dev@sha256:da26519e8fc451b15c5e1466518afb858dcef40fba2ee1f7dfdf65fce1960dd5 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r6@sha256:149af50c216f26be18e996f57d5594eca3f95ce75f7aa756315da215253cffed AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
