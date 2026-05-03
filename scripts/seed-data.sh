#!/usr/bin/env bash
set -euo pipefail

# Seed BLPS data through REST API.
# Designed for local run against Helios via SSH tunnel.
#
# Defaults:
#   BASE_URL=http://localhost:8180
#   ADMIN_USER=admin1
#   ADMIN_PASSWORD=password
#
# Usage:
#   chmod +x scripts/seed-data.sh
#   ./scripts/seed-data.sh
#   BASE_URL=http://localhost:8080 ./scripts/seed-data.sh

BASE_URL="${BASE_URL:-http://localhost:8180}"
ADMIN_USER="${ADMIN_USER:-admin1}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-password}"

require_bin() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[seed] Missing required binary: $1" >&2
    exit 1
  fi
}

require_bin curl
require_bin jq

AUTH_TOKEN=""

http_json() {
  local method="$1"
  local url="$2"
  local expected="$3"
  local data="${4:-}"
  local auth="${5:-yes}"

  local body_file
  body_file="$(mktemp)"
  local code
  local full_url="${BASE_URL}${url}"

  local -a cmd
  cmd=(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$full_url" -H "Content-Type: application/json")
  if [[ "$auth" == "yes" ]]; then
    cmd+=(-H "Authorization: Bearer ${AUTH_TOKEN}")
  fi
  if [[ -n "$data" ]]; then
    cmd+=(-d "$data")
  fi

  code="$("${cmd[@]}")"
  if [[ "$code" != "$expected" ]]; then
    echo "[seed] Request failed: $method $full_url (expected $expected, got $code)" >&2
    cat "$body_file" >&2
    rm -f "$body_file"
    exit 1
  fi

  cat "$body_file"
  rm -f "$body_file"
}

login() {
  echo "[seed] Login as admin..."
  local payload
  payload="$(jq -nc --arg username "$ADMIN_USER" --arg password "$ADMIN_PASSWORD" '{username:$username,password:$password}')"
  local resp
  resp="$(http_json "POST" "/api/v1/auth/login" "200" "$payload" "no")"
  AUTH_TOKEN="$(echo "$resp" | jq -r '.accessToken')"
  if [[ -z "$AUTH_TOKEN" || "$AUTH_TOKEN" == "null" ]]; then
    echo "[seed] Login succeeded but accessToken is missing" >&2
    echo "$resp" >&2
    exit 1
  fi
}

ensure_name_entity() {
  local list_url="$1"
  local create_url="$2"
  local name="$3"

  local found_id
  found_id="$(http_json "GET" "$list_url" "200" | jq -r --arg name "$name" '.[] | select(.name == $name) | .id' | head -n1)"
  if [[ -n "${found_id}" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc --arg name "$name" '{name:$name}')"
  http_json "POST" "$create_url" "201" "$payload" | jq -r '.id'
}

ensure_tariff() {
  local name="$1"
  local description="$2"
  local monthly_fee="$3"
  local switch_fee="$4"
  local customizable="$5"
  local pdf_url="$6"
  local category_id="$7"

  local found_id
  found_id="$(http_json "GET" "/api/v1/tariff-catalog" "200" | jq -r --arg name "$name" '.[] | select(.name == $name) | .id' | head -n1)"
  if [[ -n "$found_id" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc \
    --arg name "$name" \
    --arg description "$description" \
    --arg monthlyFee "$monthly_fee" \
    --arg switchFee "$switch_fee" \
    --argjson customizable "$customizable" \
    --arg pdfUrl "$pdf_url" \
    --argjson categoryId "$category_id" \
    '{name:$name,description:$description,monthlyFee:($monthlyFee|tonumber),switchFee:($switchFee|tonumber),customizable:$customizable,pdfUrl:$pdfUrl,categoryId:$categoryId}')"
  http_json "POST" "/api/v1/tariff-catalog" "201" "$payload" | jq -r '.id'
}

ensure_tariff_option() {
  local name="$1"
  local description="$2"
  local price="$3"
  local tariff_id="$4"

  local found_id
  found_id="$(http_json "GET" "/api/v1/tariff-options" "200" | jq -r --arg name "$name" '.[] | select(.name == $name) | .id' | head -n1)"
  if [[ -n "$found_id" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc \
    --arg name "$name" \
    --arg description "$description" \
    --arg price "$price" \
    --argjson tariffId "$tariff_id" \
    '{name:$name,description:$description,price:($price|tonumber),tariffId:$tariffId}')"
  http_json "POST" "/api/v1/tariff-options" "201" "$payload" | jq -r '.id'
}

ensure_feature() {
  local name="$1"
  local description="$2"
  local monthly_fee="$3"
  local category_id="$4"

  local found_id
  found_id="$(http_json "GET" "/api/v1/additional-features" "200" | jq -r --arg name "$name" '.[] | select(.name == $name) | .id' | head -n1)"
  if [[ -n "$found_id" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc \
    --arg name "$name" \
    --arg description "$description" \
    --arg monthlyFee "$monthly_fee" \
    --argjson categoryId "$category_id" \
    '{name:$name,description:$description,monthlyFee:($monthlyFee|tonumber),categoryId:$categoryId}')"
  http_json "POST" "/api/v1/additional-features" "201" "$payload" | jq -r '.id'
}

ensure_subscriber() {
  local phone="$1"
  local full_name="$2"
  local balance="$3"
  local tariff_id="$4"

  local found_id
  found_id="$(http_json "GET" "/api/v1/subscribers?size=200&page=0" "200" | jq -r --arg phone "$phone" '.content[]? | select(.phone == $phone) | .id' | head -n1)"
  if [[ -n "$found_id" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc \
    --arg phone "$phone" \
    --arg fullName "$full_name" \
    --arg balance "$balance" \
    --argjson currentTariffId "$tariff_id" \
    '{phone:$phone,fullName:$fullName,balance:($balance|tonumber),currentTariffId:$currentTariffId}')"
  http_json "POST" "/api/v1/subscribers" "201" "$payload" | jq -r '.id'
}

ensure_subscriber_feature() {
  local subscriber_id="$1"
  local feature_id="$2"
  local status="$3"

  local found_id
  found_id="$(http_json "GET" "/api/v1/subscriber-features?size=400&page=0" "200" | jq -r --argjson subscriberId "$subscriber_id" --argjson featureId "$feature_id" '.content[]? | select(.subscriberId == $subscriberId and .featureId == $featureId) | .id' | head -n1)"
  if [[ -n "$found_id" ]]; then
    echo "$found_id"
    return
  fi

  local payload
  payload="$(jq -nc \
    --argjson subscriberId "$subscriber_id" \
    --argjson featureId "$feature_id" \
    --arg status "$status" \
    '{subscriberId:$subscriberId,featureId:$featureId,status:$status}')"
  http_json "POST" "/api/v1/subscriber-features" "201" "$payload" | jq -r '.id'
}

main() {
  echo "[seed] BASE_URL=${BASE_URL}"
  login

  echo "[seed] Tariff categories..."
  local tariff_cat_base tariff_cat_premium
  tariff_cat_base="$(ensure_name_entity "/api/v1/tariff-categories" "/api/v1/tariff-categories" "Base")"
  tariff_cat_premium="$(ensure_name_entity "/api/v1/tariff-categories" "/api/v1/tariff-categories" "Premium")"

  echo "[seed] Tariffs..."
  local tariff_smart tariff_my
  tariff_smart="$(ensure_tariff "Smart" "Base tariff for calls and internet" "650.00" "100.00" "false" "https://mts.ru/tariffs/smart.pdf" "$tariff_cat_base")"
  tariff_my="$(ensure_tariff "My Tariff" "Customizable tariff" "900.00" "0.00" "true" "https://mts.ru/tariffs/my-tariff.pdf" "$tariff_cat_premium")"

  echo "[seed] Tariff options..."
  local opt_minutes opt_internet
  opt_minutes="$(ensure_tariff_option "Minutes +200" "Additional 200 minutes" "150.00" "$tariff_my")"
  opt_internet="$(ensure_tariff_option "Internet +20 GB" "Additional internet package" "220.00" "$tariff_my")"

  echo "[seed] Feature categories..."
  local feat_cat_safe feat_cat_fun
  feat_cat_safe="$(ensure_name_entity "/api/v1/feature-categories" "/api/v1/feature-categories" "Safety")"
  feat_cat_fun="$(ensure_name_entity "/api/v1/feature-categories" "/api/v1/feature-categories" "Entertainment")"

  echo "[seed] Additional features..."
  local feat_blocker feat_music
  feat_blocker="$(ensure_feature "Spam Blocker" "Filtering unwanted calls" "99.00" "$feat_cat_safe")"
  feat_music="$(ensure_feature "MTS Music" "Music service subscription" "169.00" "$feat_cat_fun")"

  echo "[seed] Subscriber..."
  local subscriber_id
  subscriber_id="$(ensure_subscriber "+79990000000" "Test Subscriber" "3000.00" "$tariff_smart")"

  echo "[seed] Subscriber-feature links..."
  local sub_feat_blocker sub_feat_music
  sub_feat_blocker="$(ensure_subscriber_feature "$subscriber_id" "$feat_blocker" "ACTIVE")"
  sub_feat_music="$(ensure_subscriber_feature "$subscriber_id" "$feat_music" "ACTIVE")"

  cat <<EOF
[seed] Done.
  tariff_category_base_id=$tariff_cat_base
  tariff_category_premium_id=$tariff_cat_premium
  tariff_smart_id=$tariff_smart
  tariff_my_id=$tariff_my
  tariff_option_minutes_id=$opt_minutes
  tariff_option_internet_id=$opt_internet
  feature_category_safe_id=$feat_cat_safe
  feature_category_fun_id=$feat_cat_fun
  feature_blocker_id=$feat_blocker
  feature_music_id=$feat_music
  subscriber_id=$subscriber_id
  subscriber_feature_blocker_id=$sub_feat_blocker
  subscriber_feature_music_id=$sub_feat_music
EOF
}

main "$@"
