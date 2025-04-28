 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.2

ARG BASE_IMAGE
FROM ${BASE_IMAGE:-crmdvrepo01.azurecr.io/registry.hub.docker.com/library/openjdk:21-jdk-slim}

ENV JAR_FILE_NAME=service-cp-crime-scheduleandlist-courtschedule.jar

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY build/libs/$JAR_FILE_NAME /opt/app/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 4550
RUN chmod 755 /opt/app/$JAR_FILE_NAME
CMD sh -c "java -jar /opt/app/$JAR_FILE_NAME"
