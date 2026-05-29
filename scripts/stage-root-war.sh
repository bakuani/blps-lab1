#!/usr/bin/env sh
set -eu

# Offline deployment staging for WildFly.
# Copies WAR into deployments directory and creates .dodeploy marker.
# WildFly will pick it up on next startup.

WILDFLY_HOME="${WILDFLY_HOME:-wildfly}"
DEPLOYMENTS_DIR="${DEPLOYMENTS_DIR:-$WILDFLY_HOME/standalone/deployments}"
STANDALONE_XML="${STANDALONE_XML:-$WILDFLY_HOME/standalone/configuration/standalone.xml}"
TARGET_NAME="${TARGET_NAME:-ROOT.war}"
SOURCE_WAR="${1:-}"
CLEAN_MANAGED_DEPLOYMENTS="${CLEAN_MANAGED_DEPLOYMENTS:-false}"

resolve_source_war() {
  if [ -n "$SOURCE_WAR" ]; then
    if [ ! -f "$SOURCE_WAR" ]; then
      echo "[stage] ERROR: source war not found: $SOURCE_WAR" >&2
      exit 1
    fi
    echo "$SOURCE_WAR"
    return
  fi

  if [ -f "build/libs/BLPS-0.0.1-SNAPSHOT-plain.war" ]; then
    echo "build/libs/BLPS-0.0.1-SNAPSHOT-plain.war"
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
    echo "[stage] ERROR: multiple wars found in build/libs. Pass a path explicitly." >&2
    printf "%s\n" "$wars" | sed '/^$/d' | sed 's/^/ - /' >&2
    exit 1
  fi

  echo "[stage] ERROR: war file not found. Pass it as first argument." >&2
  exit 1
}

SOURCE_WAR="$(resolve_source_war)"
SOURCE_WAR_ABS="$(cd "$(dirname "$SOURCE_WAR")" && pwd)/$(basename "$SOURCE_WAR")"

if [ ! -d "$DEPLOYMENTS_DIR" ]; then
  echo "[stage] ERROR: deployments dir not found: $DEPLOYMENTS_DIR" >&2
  exit 1
fi

TARGET_PATH="$DEPLOYMENTS_DIR/$TARGET_NAME"

echo "[stage] WildFly home: $WILDFLY_HOME"
echo "[stage] Deployments dir: $DEPLOYMENTS_DIR"
echo "[stage] Source war: $SOURCE_WAR_ABS"
echo "[stage] Target name: $TARGET_NAME"

if [ "$CLEAN_MANAGED_DEPLOYMENTS" = "true" ]; then
  if [ ! -f "$STANDALONE_XML" ]; then
    echo "[stage] ERROR: standalone.xml not found: $STANDALONE_XML" >&2
    exit 1
  fi
  backup="$STANDALONE_XML.bak.$(date +%Y%m%d%H%M%S)"
  cp "$STANDALONE_XML" "$backup"
  # Remove managed deployments block from config so boot is not blocked by stale content hashes.
  awk '
    BEGIN { skip=0 }
    /<deployments>/ { skip=1; next }
    /<\/deployments>/ { skip=0; next }
    skip==0 { print }
  ' "$backup" > "$STANDALONE_XML"
  echo "[stage] Removed <deployments> block from standalone.xml (backup: $backup)"
fi

cp "$SOURCE_WAR_ABS" "$TARGET_PATH"

# Clean previous marker files for this deployment.
rm -f "$DEPLOYMENTS_DIR/$TARGET_NAME.deployed" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.failed" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.skipdeploy" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.pending" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.undeployed" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.isdeploying" \
      "$DEPLOYMENTS_DIR/$TARGET_NAME.isundeploying"

touch "$DEPLOYMENTS_DIR/$TARGET_NAME.dodeploy"

echo "[stage] SUCCESS: staged $TARGET_NAME for auto-deploy on next WildFly start"
