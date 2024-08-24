FROM maven:3.9.9-eclipse-temurin-17@sha256:95356de0d16e1cdae757c746c7c533708be404926465bc4ba90fdc0224ce5a2e as builder
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
