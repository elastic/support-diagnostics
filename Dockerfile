FROM maven:3.9.9-eclipse-temurin-17@sha256:e260c430e6fc5296ed442eb6617ae11be9f5c5fc2f512c84df9e1a86e008e184 as builder
COPY . /support-diagnostics
WORKDIR /support-diagnostics
# TODO: fix tests and remove `-DskipTests`
RUN mvn package -DskipTests  


FROM eclipse-temurin:21@sha256:d2233012784e0b35d893f7802e28d39e39e9422180b4c6f14ed2fb714b0952e5
RUN mkdir /support-diagnostics
COPY --from=builder /support-diagnostics/scripts /support-diagnostics
COPY --from=builder /support-diagnostics/target/lib /support-diagnostics/lib
COPY --from=builder /support-diagnostics/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /support-diagnostics/src/main/resources /support-diagnostics/config

WORKDIR /support-diagnostics
