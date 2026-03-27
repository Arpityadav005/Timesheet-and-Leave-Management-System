#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"

if [[ "$SONAR_HOST_URL" == "http://localhost:9000" || "$SONAR_HOST_URL" == "http://127.0.0.1:9000" ]]; then
  SONAR_HOST_URL="http://host.docker.internal:9000"
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "SONAR_TOKEN is not set."
  echo "Create a token in SonarQube, then run:"
  echo "  export SONAR_TOKEN=your_token"
  exit 1
fi

cd "$ROOT_DIR"

modules=(
  "Config-Server"
  "EurekaServer"
  "auth-service"
  "leave-service"
  "timesheet-service"
  "admin-service"
  "api-gateway"
)

echo "Building modules for SonarQube analysis..."
echo "Installing shared common module..."
mvn -U -f "common/pom.xml" verify install

for module in "${modules[@]}"; do
  mvn -U -f "$module/pom.xml" verify
done

echo "Running SonarScanner against $SONAR_HOST_URL ..."
docker run --rm \
  -e SONAR_HOST_URL="$SONAR_HOST_URL" \
  -e SONAR_TOKEN="$SONAR_TOKEN" \
  -v "$ROOT_DIR:/usr/src" \
  sonarsource/sonar-scanner-cli:latest
