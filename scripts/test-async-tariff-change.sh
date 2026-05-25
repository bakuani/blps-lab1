#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SUBSCRIBER_ID="${SUBSCRIBER_ID:-1}"
TARGET_TARIFF_ID="${TARGET_TARIFF_ID:-2}"
TOKEN="${TOKEN:-}"

AUTH_HEADER=()
if [[ -n "$TOKEN" ]]; then
  AUTH_HEADER=(-H "Authorization: Bearer $TOKEN")
fi

echo "Submitting async tariff change request..."
response=$(curl -sS -X POST \
  "${BASE_URL}/api/v1/subscribers/${SUBSCRIBER_ID}/tariff/change" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d "{\"targetTariffId\": ${TARGET_TARIFF_ID}, \"options\": {}}")

echo "Response: $response"
request_id=$(echo "$response" | sed -n 's/.*"requestId":\([0-9]*\).*/\1/p')

if [[ -z "$request_id" ]]; then
  echo "Could not extract requestId from response"
  exit 1
fi

echo "Polling status for requestId=${request_id}..."
for _ in {1..10}; do
  status=$(curl -sS \
    "${BASE_URL}/api/v1/subscribers/${SUBSCRIBER_ID}/tariff-change-requests/${request_id}" \
    "${AUTH_HEADER[@]}")
  echo "$status"
  if echo "$status" | grep -Eq '"status":"(SUCCESS|REJECTED|FAILED)"'; then
    break
  fi
  sleep 2
done
