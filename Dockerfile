FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r5-dev@sha256:d59ebffca86e0ae84b0aaaec959e81ca02058e89d08cad5be582093c4d1945f2 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew --no-daemon build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r5@sha256:79bb5b381c6b3f617b25ab27f454d30aa2b8d7f12d1018a447d338ce1ca8f2e5 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
