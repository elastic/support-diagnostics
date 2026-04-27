FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r2-dev@sha256:7a90f97ec2453e360daa07ff41c000ceb67e22b9d1327c9e3b19df45a00d8774 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.3-r2@sha256:ef761f476e5f50b6755c17c7d679d7e08b8cca47ec3b8f12765adb7f863f0418 AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
