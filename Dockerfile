FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1-dev@sha256:7a72837159b65787e22db43f6dc9e0c26409cf2d0d331322bee0029daaa9e940 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1@sha256:9651443fff17b64b7c40279f7273d42bb37801a8bbd9e9dabb691afc9f1259a4 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
