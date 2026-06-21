#!/usr/bin/env sh
set -eu

WILDFLY_HOME="${WILDFLY_HOME:-wildfly}"
PORT_OFFSET="${PORT_OFFSET:-100}"
JBOSS_CLI="${JBOSS_CLI:-$WILDFLY_HOME/bin/jboss-cli.sh}"
JBOSS_CLI_CONTROLLER="${JBOSS_CLI_CONTROLLER:-127.0.0.1:$((9990 + PORT_OFFSET))}"
JBOSS_CLI_USER="${JBOSS_CLI_USER:-}"
JBOSS_CLI_PASSWORD="${JBOSS_CLI_PASSWORD:-}"

FORMATTER_NAME="BLPS_JSON"
HANDLER_NAME="BLPS_JSON_FILE"
LOG_FILE_NAME="${BLPS_JSON_LOG_FILE:-blps.json}"

if [ ! -x "$JBOSS_CLI" ]; then
  echo "[wildfly-logging] ERROR: jboss-cli.sh not found or not executable: $JBOSS_CLI" >&2
  exit 1
fi

run_cli() {
  command="$1"
  if [ -n "$JBOSS_CLI_USER" ] || [ -n "$JBOSS_CLI_PASSWORD" ]; then
    if [ -z "$JBOSS_CLI_USER" ] || [ -z "$JBOSS_CLI_PASSWORD" ]; then
      echo "[wildfly-logging] ERROR: set both JBOSS_CLI_USER and JBOSS_CLI_PASSWORD, or neither." >&2
      exit 1
    fi
    "$JBOSS_CLI" \
      --connect \
      --controller="$JBOSS_CLI_CONTROLLER" \
      --user="$JBOSS_CLI_USER" \
      --password="$JBOSS_CLI_PASSWORD" \
      --command="$command"
  else
    "$JBOSS_CLI" \
      --connect \
      --controller="$JBOSS_CLI_CONTROLLER" \
      --command="$command"
  fi
}

resource_exists() {
  run_cli "$1:read-resource" >/dev/null 2>&1
}

echo "[wildfly-logging] Controller: $JBOSS_CLI_CONTROLLER"
echo "[wildfly-logging] Log file: $WILDFLY_HOME/standalone/log/$LOG_FILE_NAME"

FORMATTER_ADDRESS="/subsystem=logging/json-formatter=$FORMATTER_NAME"
if resource_exists "$FORMATTER_ADDRESS"; then
  run_cli "$FORMATTER_ADDRESS:write-attribute(name=exception-output-type,value=detailed-and-formatted)"
  run_cli "$FORMATTER_ADDRESS:write-attribute(name=pretty-print,value=false)"
  run_cli "$FORMATTER_ADDRESS:write-attribute(name=print-details,value=true)"
  run_cli "$FORMATTER_ADDRESS:write-attribute(name=zone-id,value=UTC)"
else
  run_cli "$FORMATTER_ADDRESS:add(exception-output-type=detailed-and-formatted,pretty-print=false,print-details=true,zone-id=UTC)"
fi

HANDLER_ADDRESS="/subsystem=logging/periodic-rotating-file-handler=$HANDLER_NAME"
if resource_exists "$HANDLER_ADDRESS"; then
  run_cli "$HANDLER_ADDRESS:write-attribute(name=level,value=INFO)"
  run_cli "$HANDLER_ADDRESS:write-attribute(name=named-formatter,value=$FORMATTER_NAME)"
  run_cli "$HANDLER_ADDRESS:write-attribute(name=file,value={relative-to=jboss.server.log.dir,path=$LOG_FILE_NAME})"
  run_cli "$HANDLER_ADDRESS:write-attribute(name=suffix,value=\".yyyy-MM-dd\")"
else
  run_cli "$HANDLER_ADDRESS:add(append=true,autoflush=true,encoding=UTF-8,level=INFO,named-formatter=$FORMATTER_NAME,file={relative-to=jboss.server.log.dir,path=$LOG_FILE_NAME},suffix=\".yyyy-MM-dd\")"
fi

ROOT_HANDLERS="$(run_cli '/subsystem=logging/root-logger=ROOT:read-attribute(name=handlers)' || true)"
if ! printf '%s' "$ROOT_HANDLERS" | grep -q "$HANDLER_NAME"; then
  run_cli "/subsystem=logging/root-logger=ROOT:add-handler(name=$HANDLER_NAME)"
fi

APP_LOGGER_ADDRESS="/subsystem=logging/logger=ru.urasha.callmeani.blps"
if resource_exists "$APP_LOGGER_ADDRESS"; then
  run_cli "$APP_LOGGER_ADDRESS:write-attribute(name=level,value=INFO)"
  run_cli "$APP_LOGGER_ADDRESS:write-attribute(name=use-parent-handlers,value=true)"
else
  run_cli "$APP_LOGGER_ADDRESS:add(level=INFO,use-parent-handlers=true)"
fi

echo "[wildfly-logging] Reloading WildFly"
run_cli ":reload" >/dev/null 2>&1 || true
echo "[wildfly-logging] SUCCESS: JSON logging is configured"
