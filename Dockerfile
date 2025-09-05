FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7-dev@sha256:eb2acde23e1c19edc25a7e60bac50855fa62a6be6060da9aab85d683c9aa39c0 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7@sha256:7ab5f782a4e4b6a8dbb43f3f034894a23f66ff93705db9aa5d052879862c077f AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
