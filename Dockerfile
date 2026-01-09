FROM eclipse-temurin:21

WORKDIR /app

# ---- Dependencies ----
RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# ---- Application files ----
COPY docker/* /app/
COPY build/libs/*.jar /app/
COPY lib/applicationinsights.json /app/

# ---- Runtime ----
EXPOSE 4550

ENTRYPOINT ["/bin/sh","./startup.sh"]