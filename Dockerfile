FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y \
      curl \
      default-jre \
      unzip \
    && \
    apt-get clean

WORKDIR /opt/elasticsearch-support-diagnostics
RUN curl -Lo essd.zip 'https://github.com/elastic/elasticsearch-support-diagnostics/releases/download/5.11/support-diagnostics-5.11-dist.zip' && \
    unzip essd.zip

ENV JAVA_HOME=/usr/lib/jvm/default-java
WORKDIR /opt/elasticsearch-support-diagnostics/support-diagnostics-5.11
ENTRYPOINT ["/opt/elasticsearch-support-diagnostics/support-diagnostics-5.11/diagnostics.sh"]
