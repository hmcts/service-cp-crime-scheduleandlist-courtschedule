# ---- Base image (default fallback) ----
ARG BASE_IMAGE
FROM ${BASE_IMAGE:-openjdk:21-jdk-slim}

# ---- Set runtime ENV for Spring Boot to bind port
ARG SERVER_PORT
ENV SERVER_PORT=${SERVER_PORT:-4550}

# ---- Dependencies ----
RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# ---- Application files ----
COPY build/libs/*.jar /opt/app/app.jar
COPY lib/applicationinsights.json /opt/app/

# ---- Permissions ----
RUN chmod 755 /opt/app/app.jar

# ---- Runtime ----
EXPOSE 4550

CMD ["java", "-jar", "/opt/app/app.jar"]