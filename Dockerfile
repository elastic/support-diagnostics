FROM maven:3.9.9-eclipse-temurin-17@sha256:95356de0d16e1cdae757c746c7c533708be404926465bc4ba90fdc0224ce5a2e as builder
COPY . /support-diagnostics
WORKDIR /support-diagnostics
# TODO: fix tests and remove `-DskipTests`
RUN mvn package -DskipTests  


FROM eclipse-temurin:21@sha256:1b04dc7cd430939b9444293463363201011379364c4176746a771959f27d90bd
RUN mkdir /support-diagnostics
COPY --from=builder /support-diagnostics/scripts /support-diagnostics
COPY --from=builder /support-diagnostics/target/lib /support-diagnostics/lib
COPY --from=builder /support-diagnostics/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /support-diagnostics/src/main/resources /support-diagnostics/config

WORKDIR /support-diagnostics
