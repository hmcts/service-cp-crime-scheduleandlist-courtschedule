#!/bin/bash

# Define the service name
SERVICE_NAME="service-cp-crime-scheduleandlist-courtschedule"

# Check if the Docker container is running
if [ "$(docker ps -q -f name=${SERVICE_NAME})" ]; then
    echo "Stopping the running Docker container..."
    docker-compose down
fi

# Rebuild the Docker image
echo "Rebuilding the Docker image..."
docker-compose build

# Deploy the service
echo "Starting the Docker container..."
docker-compose up -d

echo "Deployment completed successfully."
