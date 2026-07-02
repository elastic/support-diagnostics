FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r5-dev@sha256:e675c0e6f55990c2cae615b3afa47c9c6bf4a3493d7db8c387850eb555327363 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r5@sha256:abd26546021574030fab674f7f34f80a90c272d0feccd868a0a63dfeb88cb014 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
