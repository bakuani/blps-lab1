#!/usr/bin/env sh
set -eu

ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://127.0.0.1:9200}"
KIBANA_URL="${KIBANA_URL:-http://127.0.0.1:5601}"
LOGSTASH_URL="${LOGSTASH_URL:-http://127.0.0.1:9600}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8180}"
BACKEND_SMOKE_PATH="${BACKEND_SMOKE_PATH:-/swagger-ui.html}"
CORRELATION_ID="${CORRELATION_ID:-elk-remote-smoke-$(date +%s)}"
ELK_EVENT_TIMEOUT_SEC="${ELK_EVENT_TIMEOUT_SEC:-30}"

echo "[elk-smoke] Checking Elasticsearch"
curl -fsS "$ELASTICSEARCH_URL/_cluster/health?pretty"

echo "[elk-smoke] Checking Kibana"
curl -fsS "$KIBANA_URL/api/status" >/dev/null

echo "[elk-smoke] Checking Logstash TCP input"
PIPELINE_STATS="$(curl -fsS "$LOGSTASH_URL/_node/stats/pipelines/main")"
if ! printf '%s' "$PIPELINE_STATS" | grep -q '"wildfly_remote_json"'; then
  echo "[elk-smoke] ERROR: Logstash input wildfly_remote_json is not active" >&2
  exit 1
fi

echo "[elk-smoke] Checking index template"
curl -fsS "$ELASTICSEARCH_URL/_index_template/blps-logs-template" >/dev/null

echo "[elk-smoke] Checking ILM policy"
curl -fsS "$ELASTICSEARCH_URL/_ilm/policy/blps-logs-policy" >/dev/null

echo "[elk-smoke] Current BLPS event count"
curl -fsS \
  -H "Content-Type: application/json" \
  "$ELASTICSEARCH_URL/blps-logs-*/_count?ignore_unavailable=true&allow_no_indices=true"

echo
echo "[elk-smoke] Sending backend request with correlation id: $CORRELATION_ID"
HTTP_STATUS="$(
  curl -sS \
    -o /dev/null \
    -w "%{http_code}" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    "${BACKEND_URL}${BACKEND_SMOKE_PATH}"
)"
echo "[elk-smoke] Backend HTTP status: $HTTP_STATUS"

elapsed=0
while [ "$elapsed" -lt "$ELK_EVENT_TIMEOUT_SEC" ]; do
  COUNT_RESPONSE="$(
    curl -fsS \
      -X POST \
      -H "Content-Type: application/json" \
      "$ELASTICSEARCH_URL/blps-logs-*/_count?ignore_unavailable=true&allow_no_indices=true" \
      --data-binary "{
        \"query\": {
          \"term\": {
            \"correlation.id\": \"$CORRELATION_ID\"
          }
        }
      }"
  )"
  COUNT="$(printf '%s' "$COUNT_RESPONSE" | sed -n 's/.*"count":\([0-9][0-9]*\).*/\1/p')"
  if [ -n "$COUNT" ] && [ "$COUNT" -gt 0 ]; then
    echo "[elk-smoke] Found $COUNT event(s) for correlation id $CORRELATION_ID"
    echo "[elk-smoke] SUCCESS"
    exit 0
  fi

  sleep 1
  elapsed=$((elapsed + 1))
done

echo "[elk-smoke] ERROR: no Elasticsearch event found for correlation id $CORRELATION_ID after ${ELK_EVENT_TIMEOUT_SEC}s" >&2
echo "[elk-smoke] Check the SSH reverse tunnel, WildFly BLPS_ELK_ASYNC handler, and Logstash TCP input." >&2
exit 1
