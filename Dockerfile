FROM eclipse-temurin:25

WORKDIR /app

# ---- Dependencies ----
RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# ---- Application files ----
COPY docker/* /app/
COPY build/libs/*.jar /app/
COPY lib/applicationinsights.json /app/
# Temp fix we need to work out the actual app user
RUN chmod 777 /opt/java/openjdk/lib/security/cacerts

# ---- Runtime ----
EXPOSE 4550

ENTRYPOINT ["/bin/sh","./startup.sh"]