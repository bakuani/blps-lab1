#!/usr/bin/env bash
set -euo pipefail

export WILDFLY_HOME="wildfly"
export PORT_OFFSET="${PORT_OFFSET:-100}"

unset SPRING_PROFILES_ACTIVE
export JWT_SECRET="9f3c8b1a7d4e2f6c0b5a9e1d3c7f2a8b6e4d1c9f0a7b3e5d8c2f6a1b4e9d7c3"
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/studs}"
export DB_USER="${DB_USER:-s413022}"
export DB_PASSWORD="${DB_PASSWORD:-1i3O5V9ts2y1GN7M}"

# RabbitMQ / JMS settings
export RABBITMQ_TUNNEL_PORT="${RABBITMQ_TUNNEL_PORT:-15673}"
export RABBITMQ_HOST="${RABBITMQ_HOST:-127.0.0.1}"
export RABBITMQ_PORT="${RABBITMQ_PORT:-$RABBITMQ_TUNNEL_PORT}"
export RABBITMQ_USER="${RABBITMQ_USER:-guest}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}"
export RABBITMQ_VHOST="${RABBITMQ_VHOST:-/}"

# Async queues and schedulers
export TARIFF_CHANGE_QUEUE="${TARIFF_CHANGE_QUEUE:-tariff.change.requests}"
export FEATURE_DISABLE_QUEUE="${FEATURE_DISABLE_QUEUE:-feature.disable.requests}"
export MONTHLY_FEE_QUEUE="${MONTHLY_FEE_QUEUE:-monthly.fee.requests}"
export MONTHLY_FEE_CRON="${MONTHLY_FEE_CRON:-*/30 * * * * *}"
export MONTHLY_FEE_CYCLE_PATTERN="${MONTHLY_FEE_CYCLE_PATTERN:-yyyy-MM-dd-HH:mm}"
export RETRY_CRON="${RETRY_CRON:-0 */1 * * * *}"

# Dolibarr EIS / JCA integration
# If Dolibarr runs locally on your laptop, expose it to helios via reverse SSH tunnel:
# helios:127.0.0.1:${EIS_DOLIBARR_TUNNEL_PORT} -> local:127.0.0.1:${EIS_DOLIBARR_LOCAL_PORT}
export EIS_DOLIBARR_TUNNEL_PORT="${EIS_DOLIBARR_TUNNEL_PORT:-28081}"
export EIS_DOLIBARR_LOCAL_PORT="${EIS_DOLIBARR_LOCAL_PORT:-8081}"
export EIS_DOLIBARR_URL="${EIS_DOLIBARR_URL:-http://127.0.0.1:${EIS_DOLIBARR_TUNNEL_PORT}}"
export EIS_DOLIBARR_API_KEY="${EIS_DOLIBARR_API_KEY:-u2m9jlo8wHJB9j6pR1H8E60GUVqsKjS3}"
export EIS_DOLIBARR_FAIL_CLOSED="${EIS_DOLIBARR_FAIL_CLOSED:-true}"
export EIS_DOLIBARR_CONNECT_TIMEOUT_MS="${EIS_DOLIBARR_CONNECT_TIMEOUT_MS:-3000}"
export EIS_DOLIBARR_READ_TIMEOUT_MS="${EIS_DOLIBARR_READ_TIMEOUT_MS:-5000}"
export EIS_DOLIBARR_AUDIT_ENABLED="${EIS_DOLIBARR_AUDIT_ENABLED:-false}"
export EIS_DOLIBARR_AUDIT_CF_JNDI="${EIS_DOLIBARR_AUDIT_CF_JNDI:-java:/eis/DolibarrConnectionFactory}"
export EIS_DOLIBARR_AUDIT_INTERACTION="${EIS_DOLIBARR_AUDIT_INTERACTION:-dolibarr.audit.operation}"

unset _JAVA_OPTIONS || true
export JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx512m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -Djava.net.preferIPv4Stack=true}"

echo "[helios-start] WILDFLY_HOME=$WILDFLY_HOME"
echo "[helios-start] DB_URL=$DB_URL"
echo "[helios-start] PORT_OFFSET=$PORT_OFFSET"
echo "[helios-start] RABBITMQ_HOST=$RABBITMQ_HOST"
echo "[helios-start] RABBITMQ_PORT=$RABBITMQ_PORT"
echo "[helios-start] EIS_DOLIBARR_FAIL_CLOSED=$EIS_DOLIBARR_FAIL_CLOSED"
echo "[helios-start] EIS_DOLIBARR_AUDIT_ENABLED=$EIS_DOLIBARR_AUDIT_ENABLED"
echo "[helios-start] EIS_DOLIBARR_AUDIT_CF_JNDI=$EIS_DOLIBARR_AUDIT_CF_JNDI"
echo "[helios-start] EIS_DOLIBARR_URL=$EIS_DOLIBARR_URL"

echo "[helios-start] Stopping previous WildFly processes (if any)..."
pkill -f 'wildfly/bin/standalone.sh' || true
pkill -f 'org.jboss.as.standalone' || true
sleep 1

echo "[helios-start] Starting WildFly..."
exec "$WILDFLY_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.socket.binding.port-offset="$PORT_OFFSET"
