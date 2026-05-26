#!/usr/bin/env sh
set -eu

WILDFLY_HOME="${WILDFLY_HOME:-wildfly}"
DEPLOYMENTS_DIR="${DEPLOYMENTS_DIR:-$WILDFLY_HOME/standalone/deployments}"
DEPLOY_TIMEOUT_SEC="${DEPLOY_TIMEOUT_SEC:-90}"
TARGET_NAME="${TARGET_NAME:-ROOT.war}"
SOURCE_WAR="${1:-}"

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

if [ ! -d "$DEPLOYMENTS_DIR" ]; then
  echo "[deploy] ERROR: deployments dir not found: $DEPLOYMENTS_DIR" >&2
  exit 1
fi

TARGET_WAR="$DEPLOYMENTS_DIR/$TARGET_NAME"

echo "[deploy] WildFly home: $WILDFLY_HOME"
echo "[deploy] Deployments dir: $DEPLOYMENTS_DIR"
echo "[deploy] Source war: $SOURCE_WAR"
echo "[deploy] Target war: $TARGET_WAR"

rm -f "$DEPLOYMENTS_DIR/$TARGET_NAME.deployed" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.failed" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.isdeploying" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.isundeploying" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.dodeploy"

cp "$SOURCE_WAR" "$TARGET_WAR"
touch "$DEPLOYMENTS_DIR/$TARGET_NAME.dodeploy"

echo "[deploy] Waiting for deployment markers (timeout: ${DEPLOY_TIMEOUT_SEC}s)..."
start_epoch="$(date +%s)"
deadline_epoch=$((start_epoch + DEPLOY_TIMEOUT_SEC))
while :; do
  now_epoch="$(date +%s)"
  if [ "$now_epoch" -ge "$deadline_epoch" ]; then
    break
  fi

  if [ -f "$DEPLOYMENTS_DIR/$TARGET_NAME.deployed" ]; then
    echo "[deploy] SUCCESS: $TARGET_NAME deployed"
    exit 0
  fi

  if [ -f "$DEPLOYMENTS_DIR/$TARGET_NAME.failed" ]; then
    echo "[deploy] ERROR: deployment failed" >&2
    cat "$DEPLOYMENTS_DIR/$TARGET_NAME.failed" >&2 || true
    exit 1
  fi

  sleep 2
done

echo "[deploy] ERROR: timed out waiting for deployment markers" >&2
ls -la "$DEPLOYMENTS_DIR/$TARGET_NAME"* 2>/dev/null || true
exit 1
