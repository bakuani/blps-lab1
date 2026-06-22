#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${SSH_HOST:-helios.se.ifmo.ru}"
SSH_PORT="${SSH_PORT:-2222}"
SSH_USER="${SSH_USER:-s413022}"

RABBITMQ_TUNNEL_PORT="${RABBITMQ_TUNNEL_PORT:-15673}"
EIS_DOLIBARR_TUNNEL_PORT="${EIS_DOLIBARR_TUNNEL_PORT:-28081}"
CAMUNDA_TUNNEL_PORT="${CAMUNDA_TUNNEL_PORT:-18080}"
BACKEND_LOCAL_PORT="${BACKEND_LOCAL_PORT:-8180}"
ELK_TUNNEL_PORT="${ELK_TUNNEL_PORT:-15044}"
ELK_LOGSTASH_LOCAL_PORT="${ELK_LOGSTASH_LOCAL_PORT:-5044}"

echo "[tunnel] Helios: ${SSH_USER}@${SSH_HOST}:${SSH_PORT}"
echo "[tunnel] RabbitMQ: helios:${RABBITMQ_TUNNEL_PORT} -> local:5672"
echo "[tunnel] Dolibarr: helios:${EIS_DOLIBARR_TUNNEL_PORT} -> local:8081"
echo "[tunnel] Camunda:  helios:${CAMUNDA_TUNNEL_PORT} -> local:8082"
echo "[tunnel] Backend:  local:${BACKEND_LOCAL_PORT} -> helios:8180"
echo "[tunnel] ELK logs: helios:${ELK_TUNNEL_PORT} -> local:${ELK_LOGSTASH_LOCAL_PORT}"

exec ssh -v -N \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -R "127.0.0.1:${RABBITMQ_TUNNEL_PORT}:127.0.0.1:5672" \
  -R "127.0.0.1:${EIS_DOLIBARR_TUNNEL_PORT}:127.0.0.1:8081" \
  -R "127.0.0.1:${CAMUNDA_TUNNEL_PORT}:127.0.0.1:8082" \
  -R "127.0.0.1:${ELK_TUNNEL_PORT}:127.0.0.1:${ELK_LOGSTASH_LOCAL_PORT}" \
  -L "127.0.0.1:${BACKEND_LOCAL_PORT}:127.0.0.1:8180" \
  -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}"
