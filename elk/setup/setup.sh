#!/bin/sh
set -eu

ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
KIBANA_URL="${KIBANA_URL:-http://kibana:5601}"

echo "[elk-setup] Installing ILM policy"
curl -fsS \
  -X PUT "${ELASTICSEARCH_URL}/_ilm/policy/blps-logs-policy" \
  -H "Content-Type: application/json" \
  --data-binary "@/setup/ilm-policy.json"

echo "[elk-setup] Installing index template"
curl -fsS \
  -X PUT "${ELASTICSEARCH_URL}/_index_template/blps-logs-template" \
  -H "Content-Type: application/json" \
  --data-binary "@/setup/index-template.json"

echo "[elk-setup] Importing Kibana data view and dashboard"
curl -fsS \
  -X POST "${KIBANA_URL}/api/saved_objects/_import?overwrite=true" \
  -H "kbn-xsrf: true" \
  --form "file=@/kibana/blps-dashboard.ndjson"

echo "[elk-setup] ELK initialization completed"
