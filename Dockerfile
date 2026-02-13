FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7-dev@sha256:ea249437c8316cf2905ea40e1508933789cf293d68b08531e7d151a779d64b25 AS builder

#####################
# Build code
#####################
WORKDIR /build

COPY ./ ./

RUN ./gradlew build

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7@sha256:a4b87ff540ce784a1b69611aa8b60b743ad70e18ef97d6a6afec4bb906d5989b AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/build/libs/diagnostics-*.jar /support-diagnostics/lib/
COPY --from=builder /build/build/lib/ /support-diagnostics/lib/
COPY --from=builder /build/src/main/resources /support-diagnostics/config
