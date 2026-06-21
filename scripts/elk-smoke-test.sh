#!/usr/bin/env sh
set -eu

ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://127.0.0.1:9200}"
KIBANA_URL="${KIBANA_URL:-http://127.0.0.1:5601}"

echo "[elk-smoke] Checking Elasticsearch"
curl -fsS "$ELASTICSEARCH_URL/_cluster/health?pretty"

echo "[elk-smoke] Checking Kibana"
curl -fsS "$KIBANA_URL/api/status" >/dev/null

echo "[elk-smoke] Checking index template"
curl -fsS "$ELASTICSEARCH_URL/_index_template/blps-logs-template" >/dev/null

echo "[elk-smoke] Checking ILM policy"
curl -fsS "$ELASTICSEARCH_URL/_ilm/policy/blps-logs-policy" >/dev/null

echo "[elk-smoke] Current BLPS event count"
curl -fsS \
  -H "Content-Type: application/json" \
  "$ELASTICSEARCH_URL/blps-logs-*/_count?ignore_unavailable=true"

echo
echo "[elk-smoke] SUCCESS"
