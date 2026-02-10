# Docker base image - note that this is currently overwritten by azure pipelines
FROM ${BASE_IMAGE:-eclipse-temurin:21-jdk}

# run as non-root ... group and user "app"
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app

# ---- Dependencies ----
RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# ---- Application files ----
COPY docker/* /app/
COPY build/libs/*.jar /app/
COPY lib/applicationinsights.json /app/

# Not sure this does anything useful we can drop once we sort certificates
RUN test -n "$JAVA_HOME" \
 && test -f "$JAVA_HOME/lib/security/cacerts" \
 && chmod 777 "$JAVA_HOME/lib/security/cacerts"

ENTRYPOINT ["/bin/sh","./startup.sh"]