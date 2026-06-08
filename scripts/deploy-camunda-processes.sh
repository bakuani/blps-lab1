#!/usr/bin/env sh
set -eu

CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:8082/engine-rest}"
PROCESS_FILE="${1:-src/main/resources/processes/blps-camunda.bpmn}"

if [ ! -f "$PROCESS_FILE" ]; then
  echo "[camunda-deploy] ERROR: process file not found: $PROCESS_FILE" >&2
  exit 1
fi

echo "[camunda-deploy] Camunda REST: $CAMUNDA_BASE_URL"
echo "[camunda-deploy] BPMN: $PROCESS_FILE"

curl -fsS \
  -X POST "$CAMUNDA_BASE_URL/deployment/create" \
  -F "deployment-name=blps-camunda-processes" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  -F "blps-camunda.bpmn=@$PROCESS_FILE;type=text/xml"

echo
echo "[camunda-deploy] SUCCESS"
