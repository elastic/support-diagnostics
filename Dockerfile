FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2-dev@sha256:c3e31a6e0a40e77092513aca4f97e7a2407bde926b2fbe7ac8ff84941fde4fbd AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r2@sha256:543b37be27afcd67637e87e5e563d712f9308b8a33e4a44194e787443f47050b AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
