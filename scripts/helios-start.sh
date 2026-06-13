#!/usr/bin/env bash
set -euo pipefail

export WILDFLY_HOME="wildfly"
export PORT_OFFSET="${PORT_OFFSET:-100}"

unset SPRING_PROFILES_ACTIVE
export JWT_SECRET="9f3c8b1a7d4e2f6c0b5a9e1d3c7f2a8b6e4d1c9f0a7b3e5d8c2f6a1b4e9d7c3"
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/studs}"
export DB_USER="${DB_USER:-s413022}"
export DB_PASSWORD="${DB_PASSWORD:-1i3O5V9ts2y1GN7M}"

export RABBITMQ_TUNNEL_PORT="${RABBITMQ_TUNNEL_PORT:-15673}"
export RABBITMQ_HOST="${RABBITMQ_HOST:-127.0.0.1}"
export RABBITMQ_PORT="${RABBITMQ_PORT:-$RABBITMQ_TUNNEL_PORT}"
export RABBITMQ_USER="${RABBITMQ_USER:-guest}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}"
export RABBITMQ_VHOST="${RABBITMQ_VHOST:-/}"

export TARIFF_CHANGE_QUEUE="${TARIFF_CHANGE_QUEUE:-tariff.change.requests}"
export FEATURE_DISABLE_QUEUE="${FEATURE_DISABLE_QUEUE:-feature.disable.requests}"
export MONTHLY_FEE_QUEUE="${MONTHLY_FEE_QUEUE:-monthly.fee.requests}"
export MONTHLY_FEE_CRON="${MONTHLY_FEE_CRON:-*/30 * * * * *}"
export MONTHLY_FEE_CYCLE_PATTERN="${MONTHLY_FEE_CYCLE_PATTERN:-yyyy-MM-dd-HH:mm}"
export RETRY_CRON="${RETRY_CRON:-0 */1 * * * *}"

export CAMUNDA_ENABLED="${CAMUNDA_ENABLED:-true}"
export CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:18080/engine-rest}"
export CAMUNDA_WORKER_ID="${CAMUNDA_WORKER_ID:-blps-helios-worker}"
export CAMUNDA_LOCK_DURATION_MS="${CAMUNDA_LOCK_DURATION_MS:-30000}"
export CAMUNDA_POLL_INTERVAL_MS="${CAMUNDA_POLL_INTERVAL_MS:-2000}"
export CAMUNDA_MAX_TASKS="${CAMUNDA_MAX_TASKS:-10}"
export MONTHLY_FEE_SCHEDULER_ENABLED="${MONTHLY_FEE_SCHEDULER_ENABLED:-false}"
export RETRY_SCHEDULER_ENABLED="${RETRY_SCHEDULER_ENABLED:-false}"

export EIS_DOLIBARR_TUNNEL_PORT="${EIS_DOLIBARR_TUNNEL_PORT:-28081}"
export EIS_DOLIBARR_LOCAL_PORT="${EIS_DOLIBARR_LOCAL_PORT:-8081}"
export EIS_DOLIBARR_URL="${EIS_DOLIBARR_URL:-http://127.0.0.1:${EIS_DOLIBARR_TUNNEL_PORT}}"
export EIS_DOLIBARR_API_KEY="${EIS_DOLIBARR_API_KEY:-udm50OXE41xVGqq90rHMGIyA5vSgc569}"
export EIS_DOLIBARR_FAIL_CLOSED="${EIS_DOLIBARR_FAIL_CLOSED:-true}"
export EIS_DOLIBARR_CONNECT_TIMEOUT_MS="${EIS_DOLIBARR_CONNECT_TIMEOUT_MS:-3000}"
export EIS_DOLIBARR_READ_TIMEOUT_MS="${EIS_DOLIBARR_READ_TIMEOUT_MS:-5000}"
export EIS_DOLIBARR_SUBSCRIBER_SYNC_ENABLED="${EIS_DOLIBARR_SUBSCRIBER_SYNC_ENABLED:-false}"
export EIS_DOLIBARR_AUDIT_ENABLED="${EIS_DOLIBARR_AUDIT_ENABLED:-true}"
export EIS_DOLIBARR_AUDIT_INTERACTION="${EIS_DOLIBARR_AUDIT_INTERACTION:-status}"

unset _JAVA_OPTIONS || true
export JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx512m -Xss256k -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -Djava.net.preferIPv4Stack=true -Dorg.springframework.boot.logging.LoggingSystem=none -Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dcom.sun.faces.enableThreading=false -Dorg.jboss.weld.bootstrap.concurrentDeployment=false -Dorg.jboss.weld.bootstrap.preloaderThreadPoolSize=0 -Dorg.jboss.weld.executor.threadPoolSize=1}"

echo "[helios-start] WILDFLY_HOME=$WILDFLY_HOME"
echo "[helios-start] DB_URL=$DB_URL"
echo "[helios-start] PORT_OFFSET=$PORT_OFFSET"
echo "[helios-start] RABBITMQ_HOST=$RABBITMQ_HOST"
echo "[helios-start] RABBITMQ_PORT=$RABBITMQ_PORT"
echo "[helios-start] EIS_DOLIBARR_FAIL_CLOSED=$EIS_DOLIBARR_FAIL_CLOSED"
echo "[helios-start] EIS_DOLIBARR_SUBSCRIBER_SYNC_ENABLED=$EIS_DOLIBARR_SUBSCRIBER_SYNC_ENABLED"
echo "[helios-start] EIS_DOLIBARR_AUDIT_ENABLED=$EIS_DOLIBARR_AUDIT_ENABLED"
echo "[helios-start] EIS_DOLIBARR_AUDIT_INTERACTION=$EIS_DOLIBARR_AUDIT_INTERACTION"
echo "[helios-start] EIS_DOLIBARR_URL=$EIS_DOLIBARR_URL"
echo "[helios-start] CAMUNDA_ENABLED=$CAMUNDA_ENABLED"
echo "[helios-start] CAMUNDA_BASE_URL=$CAMUNDA_BASE_URL"
echo "[helios-start] CAMUNDA_WORKER_ID=$CAMUNDA_WORKER_ID"
echo "[helios-start] JAVA_OPTS=$JAVA_OPTS"

echo "[helios-start] Stopping previous WildFly processes"
pkill -f 'wildfly/bin/standalone.sh' || true
pkill -f 'org.jboss.as.standalone' || true
sleep 1

echo "[helios-start] Starting WildFly..."
exec "$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.socket.binding.port-offset="$PORT_OFFSET"
