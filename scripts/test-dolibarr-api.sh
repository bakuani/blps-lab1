#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   chmod +x scripts/test-dolibarr-api.sh
#   DOLI_KEY="your_api_key" ./scripts/test-dolibarr-api.sh
#
# Optional env vars:
#   DOLI_BASE=http://localhost:8081
#   TEST_PHONE=+79990000000
#   CREATE_THIRDPARTY=1
#   THIRD_PARTY_ID=123
#   CREATE_UNPAID_INVOICE=0
#   INVOICE_AMOUNT=100
#   INVOICE_VAT=0

DOLI_BASE="${DOLI_BASE:-http://localhost:8081}"
DOLI_KEY="u2m9jlo8wHJB9j6pR1H8E60GUVqsKjS3"
TEST_PHONE="${TEST_PHONE:-+79990000000}"
CREATE_THIRDPARTY="${CREATE_THIRDPARTY:-1}"
THIRD_PARTY_ID="${THIRD_PARTY_ID:-}"
CREATE_UNPAID_INVOICE="${CREATE_UNPAID_INVOICE:-0}"
INVOICE_AMOUNT="${INVOICE_AMOUNT:-100}"
INVOICE_VAT="${INVOICE_VAT:-0}"

if [[ -z "$DOLI_KEY" ]]; then
  echo "[dolibarr-test] ERROR: DOLI_KEY is empty."
  echo "[dolibarr-test] Example: DOLI_KEY=\"<api_key>\" ./scripts/test-dolibarr-api.sh"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[dolibarr-test] ERROR: curl is required."
  exit 1
fi

HAS_JQ=0
if command -v jq >/dev/null 2>&1; then
  HAS_JQ=1
fi

TMP_BODY="$(mktemp)"
cleanup() {
  rm -f "$TMP_BODY"
}
trap cleanup EXIT

LAST_STATUS=""
LAST_BODY=""

print_body() {
  local body="$1"
  if [[ "$HAS_JQ" -eq 1 ]] && echo "$body" | jq . >/dev/null 2>&1; then
    echo "$body" | jq .
  else
    echo "$body"
  fi
}

request() {
  local label="$1"
  local method="$2"
  local url="$3"
  shift 3

  echo
  echo "========== $label =========="
  echo "[request] $method $url"

  local status
  status="$(curl -sS -o "$TMP_BODY" -w "%{http_code}" -X "$method" "$url" \
    -H "DOLAPIKEY: $DOLI_KEY" \
    "$@")"
  LAST_STATUS="$status"
  LAST_BODY="$(cat "$TMP_BODY")"

  echo "[status] $LAST_STATUS"
  echo "[body]"
  print_body "$LAST_BODY"
}

echo "[dolibarr-test] DOLI_BASE=$DOLI_BASE"
echo "[dolibarr-test] CREATE_THIRDPARTY=$CREATE_THIRDPARTY"
echo "[dolibarr-test] CREATE_UNPAID_INVOICE=$CREATE_UNPAID_INVOICE"
echo "[dolibarr-test] TEST_PHONE=$TEST_PHONE"

request "1) Status" "GET" "$DOLI_BASE/api/index.php/status"

request "2) List thirdparties" "GET" "$DOLI_BASE/api/index.php/thirdparties?limit=10"

if [[ -z "$THIRD_PARTY_ID" && "$HAS_JQ" -eq 1 ]]; then
  # If list endpoint returned non-empty array, pick first id.
  THIRD_PARTY_ID="$(echo "$LAST_BODY" | jq -r 'if type=="array" and length>0 then .[0].id // .[0].rowid // empty else empty end' 2>/dev/null || true)"
fi

if [[ "$CREATE_THIRDPARTY" == "1" ]]; then
  PAYLOAD="$(cat <<JSON
{
  "name": "BLPS Test Client $(date +%s)",
  "client": 1,
  "phone": "$TEST_PHONE"
}
JSON
)"
  request "3) Create thirdparty" "POST" "$DOLI_BASE/api/index.php/thirdparties" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD"

  if [[ "$HAS_JQ" -eq 1 ]]; then
    # Dolibarr often returns numeric id as raw JSON number.
    CREATED_ID="$(echo "$LAST_BODY" | jq -r 'if type=="number" then . else .id // .rowid // empty end' 2>/dev/null || true)"
    if [[ -n "$CREATED_ID" && "$CREATED_ID" != "null" ]]; then
      THIRD_PARTY_ID="$CREATED_ID"
      echo "[dolibarr-test] THIRD_PARTY_ID detected from create response: $THIRD_PARTY_ID"
    fi
  else
    # Fallback: if body is plain digits.
    if [[ "$LAST_BODY" =~ ^[0-9]+$ ]]; then
      THIRD_PARTY_ID="$LAST_BODY"
      echo "[dolibarr-test] THIRD_PARTY_ID detected from create response: $THIRD_PARTY_ID"
    fi
  fi
fi

if [[ -n "$THIRD_PARTY_ID" ]]; then
  request "4) Get thirdparty by id" "GET" "$DOLI_BASE/api/index.php/thirdparties/$THIRD_PARTY_ID"

  if [[ "$CREATE_UNPAID_INVOICE" == "1" ]]; then
    echo
    echo "[dolibarr-test] Attempting to create unpaid invoice for thirdparty_id=$THIRD_PARTY_ID"

    INVOICE_PAYLOAD="$(cat <<JSON
{
  "socid": $THIRD_PARTY_ID,
  "type": 0
}
JSON
)"
    request "5) Create invoice draft" "POST" "$DOLI_BASE/api/index.php/invoices" \
      -H "Content-Type: application/json" \
      -d "$INVOICE_PAYLOAD"

    INVOICE_ID=""
    if [[ "$HAS_JQ" -eq 1 ]]; then
      INVOICE_ID="$(echo "$LAST_BODY" | jq -r 'if type=="number" then . else .id // .rowid // empty end' 2>/dev/null || true)"
    elif [[ "$LAST_BODY" =~ ^[0-9]+$ ]]; then
      INVOICE_ID="$LAST_BODY"
    fi

    if [[ -n "$INVOICE_ID" && "$INVOICE_ID" != "null" ]]; then
      echo "[dolibarr-test] INVOICE_ID detected: $INVOICE_ID"

      LINE_PAYLOAD="$(cat <<JSON
{
  "desc": "BLPS unpaid invoice test $(date +%s)",
  "subprice": $INVOICE_AMOUNT,
  "qty": 1,
  "tva_tx": $INVOICE_VAT
}
JSON
)"
      request "6) Add invoice line" "POST" "$DOLI_BASE/api/index.php/invoices/$INVOICE_ID/lines" \
        -H "Content-Type: application/json" \
        -d "$LINE_PAYLOAD"

      request "7) Validate invoice (leave unpaid)" "POST" "$DOLI_BASE/api/index.php/invoices/$INVOICE_ID/validate"
    else
      echo "[dolibarr-test] Could not parse INVOICE_ID from create response, skipping line/validate steps."
    fi
  fi

  request "5) Invoices by thirdparty" "GET" \
    "$DOLI_BASE/api/index.php/invoices?thirdparty_ids=$THIRD_PARTY_ID&limit=20"

  request "6) Unpaid invoices by thirdparty" "GET" \
    "$DOLI_BASE/api/index.php/invoices?thirdparty_ids=$THIRD_PARTY_ID&status=unpaid&limit=20"
else
  echo
  echo "[dolibarr-test] THIRD_PARTY_ID is empty, skipping invoice checks."
  echo "[dolibarr-test] Provide THIRD_PARTY_ID=<id> or run with CREATE_THIRDPARTY=1 and jq installed."
fi

echo
echo "[dolibarr-test] Done."
