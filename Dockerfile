FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3-dev@sha256:e903023447edb1ca8185f3d72874b451501cdcab407fcd2b213df6bd79a89ed2 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r3@sha256:9a5b5720b38ab121219dda2fb992d3c5670fc97b09f1d55c76e242a05974343f AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
