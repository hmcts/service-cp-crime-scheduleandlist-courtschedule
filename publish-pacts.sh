#!/bin/bash

# Load env vars from .env if available
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

# Export Git metadata
export GIT_COMMIT=$(git rev-parse HEAD)

if [ -n "$GIT_BRANCH" ]; then
  BRANCH_NAME="$GIT_BRANCH"
else
  BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)
fi

export GIT_BRANCH="$BRANCH_NAME"

# Run provider verification with results published
gradle pactVerificationTest \
  -Dpact.provider.version="$GIT_COMMIT" \
  -Dpact.verifier.publishResults=true \
  -Dpact.provider.branch="$GIT_BRANCH" \
  -DPACT_BROKER_TOKEN="$PACT_BROKER_TOKEN" \
  -DPACT_BROKER_HOST="$PACT_BROKER_URL"

# Tag provider in the broker
pact-broker create-version-tag \
  --pacticipant "VPCourtScheduleProvider" \
  --version "$GIT_COMMIT" \
  --tag "$PACT_ENV" \
  --broker-base-url "$PACT_BROKER_URL" \
  --broker-token "$PACT_BROKER_TOKEN"

# Can-I-Deploy check
echo "Running can-i-deploy check for provider compatibility..."
pact-broker can-i-deploy \
  --pacticipant "VPCourtScheduleProvider" \
  --version "$GIT_COMMIT" \
  --to-environment "$PACT_ENV" \
  --broker-base-url "$PACT_BROKER_URL" \
  --broker-token "$PACT_BROKER_TOKEN" \
  --output table

CAN_I_DEPLOY_STATUS=$?

if [ "$CAN_I_DEPLOY_STATUS" -ne 0 ]; then
  echo "❌ Can-I-Deploy failed — this version is NOT safe to deploy to $PACT_ENV."
  exit 1
else
  echo "✅ Can-I-Deploy succeeded — this version is safe to deploy to $PACT_ENV."
fi