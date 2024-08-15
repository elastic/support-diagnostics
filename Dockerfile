FROM maven:3.9.8-eclipse-temurin-17@sha256:411266e0bddf6be39c6fada1c8dc144468a2badbeb32e8effb5b11f0c5a76c33 as builder
COPY . /support-diagnostics
WORKDIR /support-diagnostics
# TODO: fix tests and remove `-DskipTests`
RUN mvn package -DskipTests  


FROM eclipse-temurin:17.0.12_7-jre@sha256:472608870ca524f9dfcf3405a7e9252c4a8bb9d96af4fc4c11b9f5a044da0749
RUN mkdir /support-diagnostics
COPY --from=builder /support-diagnostics/scripts /support-diagnostics
COPY --from=builder /support-diagnostics/target/lib /support-diagnostics/lib
COPY --from=builder /support-diagnostics/target/diagnostics-*.jar /support-diagnostics/lib
COPY --from=builder /support-diagnostics/src/main/resources /support-diagnostics/config

WORKDIR /support-diagnostics
