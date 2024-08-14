FROM maven:3.9.0-eclipse-temurin-17@sha256:8d19f7daf6e637d8ff45314861f964b08cee4279d8230c9fc4e4cc002e1c16e4 as builder
COPY . /support-diagnostics
WORKDIR /support-diagnostics
# TODO: fix tests and remove `-DskipTests`
RUN mvn package -DskipTests  


FROM eclipse-temurin:17.0.1_12-jre@sha256:51c2f3da38ce100fd8066315b6870061d2700068e41c63cbbbdb8ab94d32d8d3
RUN mkdir /support-diagnostics
COPY --from=builder /support-diagnostics/scripts /support-diagnostics
COPY --from=builder /support-diagnostics/target/lib /support-diagnostics/lib
COPY --from=builder /support-diagnostics/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /support-diagnostics/src/main/resources /support-diagnostics/config

WORKDIR /support-diagnostics
