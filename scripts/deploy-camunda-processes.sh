#!/usr/bin/env bash
set -euo pipefail

CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:8082/engine-rest}"
PROCESS_TARGET="${1:-src/main/resources/processes}"
DEPLOYMENT_NAME="${CAMUNDA_DEPLOYMENT_NAME:-blps-camunda-processes}"

if [ -d "$PROCESS_TARGET" ]; then
  shopt -s nullglob
  PROCESS_FILES=("$PROCESS_TARGET"/*.bpmn)
else
  PROCESS_FILES=("$PROCESS_TARGET")
fi

if [ "${#PROCESS_FILES[@]}" -eq 0 ]; then
  echo "[camunda-deploy] ERROR: no BPMN files found in: $PROCESS_TARGET" >&2
  exit 1
fi

for process_file in "${PROCESS_FILES[@]}"; do
  if [ ! -f "$process_file" ]; then
    echo "[camunda-deploy] ERROR: process file not found: $process_file" >&2
    exit 1
  fi
done

curl_args=(
  -fsS
  -X POST "$CAMUNDA_BASE_URL/deployment/create"
  -F "deployment-name=$DEPLOYMENT_NAME"
  -F "enable-duplicate-filtering=true"
  -F "deploy-changed-only=true"
)

echo "[camunda-deploy] Camunda REST: $CAMUNDA_BASE_URL"
echo "[camunda-deploy] BPMN files:"
for process_file in "${PROCESS_FILES[@]}"; do
  resource_name="$(basename "$process_file")"
  echo "[camunda-deploy]   - $process_file"
  curl_args+=(-F "$resource_name=@$process_file;type=text/xml")
done

if ! command -v curl >/dev/null 2>&1; then
  echo "[camunda-deploy] ERROR: curl is not installed" >&2
  exit 1
fi

curl "${curl_args[@]}"

echo
echo "[camunda-deploy] SUCCESS"
