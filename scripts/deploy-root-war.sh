#!/usr/bin/env sh
set -eu

WILDFLY_HOME="${WILDFLY_HOME:-wildfly}"
PORT_OFFSET="${PORT_OFFSET:-100}"
JBOSS_CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
DEPLOY_TIMEOUT_SEC="${DEPLOY_TIMEOUT_SEC:-90}"
TARGET_NAME="${TARGET_NAME:-ROOT.war}"
SOURCE_WAR="${1:-}"

default_controller_port=$((9990 + PORT_OFFSET))
JBOSS_CLI_CONTROLLER="${JBOSS_CLI_CONTROLLER:-127.0.0.1:${default_controller_port}}"
JBOSS_CLI_USER="${JBOSS_CLI_USER:-}"
JBOSS_CLI_PASSWORD="${JBOSS_CLI_PASSWORD:-}"

resolve_source_war() {
  if [ -n "$SOURCE_WAR" ]; then
    if [ ! -f "$SOURCE_WAR" ]; then
      echo "[deploy] ERROR: source war not found: $SOURCE_WAR" >&2
      exit 1
    fi
    echo "$SOURCE_WAR"
    return
  fi

  if [ -f "ROOT.war" ]; then
    echo "ROOT.war"
    return
  fi

  wars="$(find build/libs -maxdepth 1 -type f -name "*.war" 2>/dev/null | sort || true)"
  war_count="$(printf "%s\n" "$wars" | sed '/^$/d' | wc -l | tr -d ' ')"
  if [ "$war_count" = "1" ]; then
    printf "%s\n" "$wars" | sed '/^$/d' | head -n 1
    return
  fi

  if [ "$war_count" -gt 1 ] 2>/dev/null; then
    echo "[deploy] ERROR: multiple wars found in build/libs. Pass a path explicitly." >&2
    printf "%s\n" "$wars" | sed '/^$/d' | sed 's/^/ - /' >&2
    exit 1
  fi

  echo "[deploy] ERROR: war file not found. Pass it as first argument, e.g. ./scripts/deploy-root-war.sh build/libs/app.war" >&2
  exit 1
}

SOURCE_WAR="$(resolve_source_war)"
SOURCE_WAR_ABS="$(cd "$(dirname "$SOURCE_WAR")" && pwd)/$(basename "$SOURCE_WAR")"

if [ ! -x "$JBOSS_CLI" ]; then
  echo "[deploy] ERROR: jboss-cli.sh not found or not executable: $JBOSS_CLI" >&2
  echo "[deploy] Set WILDFLY_HOME to your WildFly root directory." >&2
  exit 1
fi

echo "[deploy] WildFly home: $WILDFLY_HOME"
echo "[deploy] CLI: $JBOSS_CLI"
echo "[deploy] Controller: $JBOSS_CLI_CONTROLLER"
echo "[deploy] Source war: $SOURCE_WAR_ABS"
echo "[deploy] Target deployment name: $TARGET_NAME"

CLI_COMMAND="deploy \"$SOURCE_WAR_ABS\" --name=$TARGET_NAME --runtime-name=$TARGET_NAME --force"

if [ -n "$JBOSS_CLI_USER" ] || [ -n "$JBOSS_CLI_PASSWORD" ]; then
  if [ -z "$JBOSS_CLI_USER" ] || [ -z "$JBOSS_CLI_PASSWORD" ]; then
    echo "[deploy] ERROR: set both JBOSS_CLI_USER and JBOSS_CLI_PASSWORD, or neither." >&2
    exit 1
  fi
  "$JBOSS_CLI" \
    --connect \
    --controller="$JBOSS_CLI_CONTROLLER" \
    --user="$JBOSS_CLI_USER" \
    --password="$JBOSS_CLI_PASSWORD" \
    --command-timeout="$DEPLOY_TIMEOUT_SEC" \
    --command="$CLI_COMMAND"
else
  "$JBOSS_CLI" \
    --connect \
    --controller="$JBOSS_CLI_CONTROLLER" \
    --command-timeout="$DEPLOY_TIMEOUT_SEC" \
    --command="$CLI_COMMAND"
fi

echo "[deploy] SUCCESS: deployed $TARGET_NAME via jboss-cli"
