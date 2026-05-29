#!/usr/bin/env bash
set -euo pipefail

echo "1) Login"
LOGIN_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"password"}')
echo "$LOGIN_RESPONSE"
TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
echo

echo "2) Create tariff category Base"
BASE_CATEGORY_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/tariff-categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Base"}')
echo "$BASE_CATEGORY_RESPONSE"
BASE_CATEGORY_ID=$(echo "$BASE_CATEGORY_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "3) Create tariff category Premium"
PREMIUM_CATEGORY_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/tariff-categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Premium"}')
echo "$PREMIUM_CATEGORY_RESPONSE"
PREMIUM_CATEGORY_ID=$(echo "$PREMIUM_CATEGORY_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "4) Create tariff Smart"
SMART_TARIFF_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/tariff-catalog" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Smart\",\"description\":\"Base tariff\",\"monthlyFee\":650.00,\"switchFee\":100.00,\"customizable\":false,\"pdfUrl\":\"https://mts.ru/tariffs/smart.pdf\",\"categoryId\":$BASE_CATEGORY_ID}")
echo "$SMART_TARIFF_RESPONSE"
SMART_TARIFF_ID=$(echo "$SMART_TARIFF_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "5) Create tariff My Tariff"
MY_TARIFF_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/tariff-catalog" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"My Tariff\",\"description\":\"Custom tariff\",\"monthlyFee\":900.00,\"switchFee\":0.00,\"customizable\":true,\"pdfUrl\":\"https://mts.ru/tariffs/my-tariff.pdf\",\"categoryId\":$PREMIUM_CATEGORY_ID}")
echo "$MY_TARIFF_RESPONSE"
MY_TARIFF_ID=$(echo "$MY_TARIFF_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "6) Create tariff option Minutes +200"
curl -sS -X POST "http://localhost:8180/api/v1/tariff-options" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Minutes +200\",\"description\":\"Additional 200 minutes\",\"price\":150.00,\"tariffId\":$MY_TARIFF_ID}"
echo
echo

echo "7) Create tariff option Internet +20 GB"
curl -sS -X POST "http://localhost:8180/api/v1/tariff-options" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Internet +20 GB\",\"description\":\"Additional internet\",\"price\":220.00,\"tariffId\":$MY_TARIFF_ID}"
echo
echo

echo "8) Create feature category Safety"
SAFETY_CATEGORY_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/feature-categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Safety"}')
echo "$SAFETY_CATEGORY_RESPONSE"
SAFETY_CATEGORY_ID=$(echo "$SAFETY_CATEGORY_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "9) Create feature category Entertainment"
ENT_CATEGORY_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/feature-categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Entertainment"}')
echo "$ENT_CATEGORY_RESPONSE"
ENT_CATEGORY_ID=$(echo "$ENT_CATEGORY_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "10) Create feature Spam Blocker"
BLOCKER_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/additional-features" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Spam Blocker\",\"description\":\"Filtering unwanted calls\",\"monthlyFee\":99.00,\"categoryId\":$SAFETY_CATEGORY_ID}")
echo "$BLOCKER_RESPONSE"
BLOCKER_ID=$(echo "$BLOCKER_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "11) Create feature MTS Music"
MUSIC_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/additional-features" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"MTS Music\",\"description\":\"Music service\",\"monthlyFee\":169.00,\"categoryId\":$ENT_CATEGORY_ID}")
echo "$MUSIC_RESPONSE"
MUSIC_ID=$(echo "$MUSIC_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "12) Create subscriber"
SUBSCRIBER_RESPONSE=$(curl -sS -X POST "http://localhost:8180/api/v1/subscribers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"+79990000000\",\"fullName\":\"Test Subscriber\",\"balance\":3000.00,\"currentTariffId\":$SMART_TARIFF_ID}")
echo "$SUBSCRIBER_RESPONSE"
SUBSCRIBER_ID=$(echo "$SUBSCRIBER_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo

echo "13) Attach Spam Blocker to subscriber"
curl -sS -X POST "http://localhost:8180/api/v1/subscriber-features" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"subscriberId\":$SUBSCRIBER_ID,\"featureId\":$BLOCKER_ID,\"status\":\"ACTIVE\"}"
echo
echo

echo "14) Attach MTS Music to subscriber"
curl -sS -X POST "http://localhost:8180/api/v1/subscriber-features" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"subscriberId\":$SUBSCRIBER_ID,\"featureId\":$MUSIC_ID,\"status\":\"ACTIVE\"}"
echo
echo

echo "Done."
