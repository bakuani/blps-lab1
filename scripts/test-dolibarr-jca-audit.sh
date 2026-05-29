#!/usr/bin/env bash
set -euo pipefail

echo "1) Dolibarr status"
curl -sS -H "DOLAPIKEY: u2m9jlo8wHJB9j6pR1H8E60GUVqsKjS3" "http://localhost:8081/api/index.php/status"
echo
echo

echo "2) Dolibarr thirdparties (может вернуть 404, если список пуст)"
curl -sS -H "DOLAPIKEY: u2m9jlo8wHJB9j6pR1H8E60GUVqsKjS3" "http://localhost:8081/api/index.php/thirdparties?limit=5"
echo
echo

echo "3) Login to BLPS"
LOGIN_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"password"}')
echo "$LOGIN_RESPONSE"
TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

echo

echo "4) Submit async tariff change (triggers EIS validation + audit)"
TARIFF_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/subscribers/1/tariff/change" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetTariffId":2,"options":{}}')
echo "$TARIFF_RESPONSE"
TARIFF_REQUEST_ID=$(echo "$TARIFF_RESPONSE" | sed -n 's/.*"requestId":\([0-9]*\).*/\1/p')

if [ -z "$TARIFF_REQUEST_ID" ]; then
  echo "ERROR: tariff requestId not found"
  exit 1
fi
echo

echo "5) Poll tariff request status"
for _ in 1 2 3 4 5 6 7 8 9 10; do
  STATUS_RESPONSE=$(curl -sS "http://localhost:8180/api/v1/subscribers/1/tariff-change-requests/$TARIFF_REQUEST_ID" \
    -H "Authorization: Bearer $TOKEN")
  echo "$STATUS_RESPONSE"
  if echo "$STATUS_RESPONSE" | grep -Eq '"status":"(SUCCESS|REJECTED|FAILED)"'; then
    break
  fi
  sleep 2
done
echo

echo "6) Submit async feature disable (second audit event)"
FEATURE_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/subscribers/1/features/1/disable" \
  -H "Authorization: Bearer $TOKEN")
echo "$FEATURE_RESPONSE"
FEATURE_REQUEST_ID=$(echo "$FEATURE_RESPONSE" | sed -n 's/.*"requestId":\([0-9]*\).*/\1/p')

if [ -z "$FEATURE_REQUEST_ID" ]; then
  echo "ERROR: feature requestId not found"
  exit 1
fi
echo

echo "7) Poll feature disable status"
for _ in 1 2 3 4 5 6 7 8 9 10; do
  STATUS_RESPONSE=$(curl -sS "http://localhost:8180/api/v1/subscribers/1/feature-disable-requests/$FEATURE_REQUEST_ID" \
    -H "Authorization: Bearer $TOKEN")
  echo "$STATUS_RESPONSE"
  if echo "$STATUS_RESPONSE" | grep -Eq '"status":"(SUCCESS|REJECTED|FAILED)"'; then
    break
  fi
  sleep 2
done
echo

echo "DONE"

