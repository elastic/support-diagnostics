FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1-dev@sha256:02fc28092c9040c77ab16411148f859077e2bb7a97de8a36a8ee323aa7384fa7 AS builder

#####################
# Build code
#####################
USER root

WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-25.0.2-r1@sha256:b398c2e0383191e11c75683c04d9b36f0ecf734f04bb56c855d7e04c306bdc4e AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
