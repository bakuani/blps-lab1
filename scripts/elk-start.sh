#!/usr/bin/env sh
set -eu

docker compose up -d

echo "[elk-start] Elasticsearch: http://127.0.0.1:9200"
echo "[elk-start] Kibana:        http://127.0.0.1:5601"
echo "[elk-start] Logstash API:  http://127.0.0.1:9600"
echo "[elk-start] Wait until blps-elk-setup exits with code 0."
