#!/usr/bin/env sh
set -eu

# Run this script on helios (the host where WildFly is running).
# It verifies that async flow reaches terminal status and checks server.log
# for Dolibarr audit lines with exact requestId values.

BASE_URL="${BASE_URL:-http://127.0.0.1:8180}"
SERVER_LOG="${SERVER_LOG:-wildfly/standalone/log/server.log}"
USERNAME="${USERNAME:-admin1}"
PASSWORD="${PASSWORD:-password}"
SUBSCRIBER_ID="${SUBSCRIBER_ID:-1}"
TARIFF_ID="${TARIFF_ID:-2}"
FEATURE_ID="${FEATURE_ID:-1}"
POLL_TRIES="${POLL_TRIES:-20}"
POLL_SLEEP_SEC="${POLL_SLEEP_SEC:-2}"
AUDIT_LOG_WAIT_SEC="${AUDIT_LOG_WAIT_SEC:-3}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[probe] ERROR: command '$1' not found" >&2
    exit 1
  fi
}

json_get_string() {
  key="$1"
  json="$2"
  if command -v jq >/dev/null 2>&1; then
    printf "%s" "$json" | jq -r --arg k "$key" '.[$k] // empty'
  else
    printf "%s" "$json" | tr -d '\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p"
  fi
}

json_get_number() {
  key="$1"
  json="$2"
  if command -v jq >/dev/null 2>&1; then
    printf "%s" "$json" | jq -r --arg k "$key" '.[$k] // empty'
  else
    printf "%s" "$json" | tr -d '\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\\([0-9][0-9]*\\).*/\\1/p"
  fi
}

poll_terminal_status() {
  endpoint="$1"
  token="$2"
  i=1
  while [ "$i" -le "$POLL_TRIES" ]; do
    body="$(curl -sS "$BASE_URL$endpoint" -H "Authorization: Bearer $token")"
    status="$(json_get_string status "$body")"
    echo "[probe] poll #$i status=$status body=$body"
    case "$status" in
      SUCCESS|REJECTED|FAILED)
        printf "%s" "$status"
        return 0
        ;;
    esac
    i=$((i + 1))
    sleep "$POLL_SLEEP_SEC"
  done
  return 1
}

require_cmd curl
require_cmd sed
require_cmd grep
require_cmd wc
require_cmd tail

if [ -f "$SERVER_LOG" ]; then
  LOG_BASELINE_LINES="$(wc -l < "$SERVER_LOG" | tr -d ' ')"
else
  LOG_BASELINE_LINES="0"
fi

echo "[probe] BASE_URL=$BASE_URL"
echo "[probe] SERVER_LOG=$SERVER_LOG"
echo "[probe] baseline log lines=$LOG_BASELINE_LINES"

echo "[probe] 1) Login"
LOGIN_RESPONSE="$(curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")"
TOKEN="$(json_get_string accessToken "$LOGIN_RESPONSE")"
if [ -z "$TOKEN" ]; then
  echo "[probe] ERROR: accessToken not found in login response: $LOGIN_RESPONSE" >&2
  exit 1
fi

echo "[probe] 2) Submit async tariff change"
TARIFF_RESPONSE="$(curl -sS -X POST "$BASE_URL/api/v1/subscribers/$SUBSCRIBER_ID/tariff/change" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"targetTariffId\":$TARIFF_ID,\"options\":{}}")"
TARIFF_REQUEST_ID="$(json_get_number requestId "$TARIFF_RESPONSE")"
if [ -z "$TARIFF_REQUEST_ID" ]; then
  echo "[probe] ERROR: tariff requestId not found: $TARIFF_RESPONSE" >&2
  exit 1
fi
echo "[probe] tariff requestId=$TARIFF_REQUEST_ID"

echo "[probe] 3) Poll tariff status"
if ! poll_terminal_status "/api/v1/subscribers/$SUBSCRIBER_ID/tariff-change-requests/$TARIFF_REQUEST_ID" "$TOKEN" >/dev/null; then
  echo "[probe] ERROR: tariff request did not reach terminal status" >&2
  exit 1
fi

echo "[probe] 4) Submit async feature disable"
FEATURE_RESPONSE="$(curl -sS -X POST "$BASE_URL/api/v1/subscribers/$SUBSCRIBER_ID/features/$FEATURE_ID/disable" \
  -H "Authorization: Bearer $TOKEN")"
FEATURE_REQUEST_ID="$(json_get_number requestId "$FEATURE_RESPONSE")"
if [ -z "$FEATURE_REQUEST_ID" ]; then
  echo "[probe] ERROR: feature requestId not found: $FEATURE_RESPONSE" >&2
  exit 1
fi
echo "[probe] feature requestId=$FEATURE_REQUEST_ID"

echo "[probe] 5) Poll feature status"
if ! poll_terminal_status "/api/v1/subscribers/$SUBSCRIBER_ID/feature-disable-requests/$FEATURE_REQUEST_ID" "$TOKEN" >/dev/null; then
  echo "[probe] ERROR: feature request did not reach terminal status" >&2
  exit 1
fi

echo "[probe] 6) Wait for audit logs (${AUDIT_LOG_WAIT_SEC}s)"
sleep "$AUDIT_LOG_WAIT_SEC"

if [ ! -f "$SERVER_LOG" ]; then
  echo "[probe] WARN: server log not found at '$SERVER_LOG' (cannot verify audit logs)." >&2
  exit 2
fi

START_LINE=$((LOG_BASELINE_LINES + 1))
TMP_LOG="$(mktemp)"
tail -n "+$START_LINE" "$SERVER_LOG" > "$TMP_LOG" || true

echo "[probe] 7) Search for Dolibarr audit lines"
MATCHED=0
if grep -E "Dolibarr audit|requestId=$TARIFF_REQUEST_ID|requestId=$FEATURE_REQUEST_ID" "$TMP_LOG" >/dev/null 2>&1; then
  MATCHED=1
  grep -E "Dolibarr audit|requestId=$TARIFF_REQUEST_ID|requestId=$FEATURE_REQUEST_ID" "$TMP_LOG" || true
fi

rm -f "$TMP_LOG"

if [ "$MATCHED" -eq 1 ]; then
  echo "[probe] SUCCESS: audit-related log lines were found."
  exit 0
fi

echo "[probe] ERROR: no audit-related lines found in new server log fragment." >&2
echo "[probe] Hint: check EIS_DOLIBARR_AUDIT_ENABLED, JMS connectivity, and logging config." >&2
exit 2
