FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7-dev@sha256:fad9af16f5917c514e6a7f76be0933c5f86e739f2a4956cd33efe5302e79ef73 AS builder

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

FROM docker.elastic.co/wolfi/jdk:openjdk-23.0.2-r7@sha256:58d7573b5dad66d112cdd190785e9482de6a03e8aad5d06d4d451e5c1fd1194b AS runner

########################
# Prepare the code to run
########################
WORKDIR /support-diagnostics

COPY --from=builder /build/scripts /support-diagnostics
COPY --from=builder /build/target/lib /support-diagnostics/lib
COPY --from=builder /build/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /build/src/main/resources /support-diagnostics/config
