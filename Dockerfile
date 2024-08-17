FROM maven:3.9.8-eclipse-temurin-17@sha256:411266e0bddf6be39c6fada1c8dc144468a2badbeb32e8effb5b11f0c5a76c33 as builder
COPY . /support-diagnostics
WORKDIR /support-diagnostics
# TODO: fix tests and remove `-DskipTests`
RUN mvn package -DskipTests  


FROM eclipse-temurin:17.0.12_7-jre@sha256:ff6b565d2c0b9050faa7633344421bf09ba3547c8d17e9a85416cbd6a1b66450
RUN mkdir /support-diagnostics
COPY --from=builder /support-diagnostics/scripts /support-diagnostics
COPY --from=builder /support-diagnostics/target/lib /support-diagnostics/lib
COPY --from=builder /support-diagnostics/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /support-diagnostics/src/main/resources /support-diagnostics/config

WORKDIR /support-diagnostics
