#!/bin/bash

# Default values
BASE_URL="http://localhost:8080"
echo "Initializing database via $BASE_URL..."

# 1. Create Base Tariff Category
echo "1. Creating Base Tariff Category..."
curl -s -X POST "$BASE_URL/api/v1/admin/tariff-categories" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Базовые"
}' | jq || echo

# 2. Create Premium Tariff Category
echo "2. Creating Premium Tariff Category..."
curl -s -X POST "$BASE_URL/api/v1/admin/tariff-categories" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Премиум"
}' | jq || echo

# 3. Create Smart Tariff
echo "3. Creating Smart Tariff..."
curl -s -X POST "$BASE_URL/api/v1/admin/tariffs" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Smart",
  "description": "Базовый тариф для звонков и интернета",
  "monthlyFee": 650.00,
  "switchFee": 100.00,
  "customizable": false,
  "pdfUrl": "https://mts.ru/tariffs/smart.pdf",
  "categoryId": 1
}' | jq || echo

# 4. Create My Tariff
echo "4. Creating My Tariff..."
curl -s -X POST "$BASE_URL/api/v1/admin/tariffs" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Мой Тариф",
  "description": "Кастомизируемый тариф",
  "monthlyFee": 900.00,
  "switchFee": 0.00,
  "customizable": true,
  "pdfUrl": "https://mts.ru/tariffs/my-tariff.pdf",
  "categoryId": 2
}' | jq || echo

# 5. Create Tariff Option Minutes
echo "5. Creating Tariff Option Minutes..."
curl -s -X POST "$BASE_URL/api/v1/admin/tariff-options" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Пакет минут +200",
  "description": "Дополнительные 200 минут",
  "price": 150.00,
  "tariffId": 2
}' | jq || echo

# 6. Create Service Category (Entertainment)
echo "6. Creating Service Category..."
curl -s -X POST "$BASE_URL/api/v1/admin/service-categories" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "Развлечения"
}' | jq || echo

# 7. Create Additional Service (MTS Music)
echo "7. Creating Additional Service..."
curl -s -X POST "$BASE_URL/api/v1/admin/services" \
  -H "Content-Type: application/json" \
  -d '{
  "name": "MTS Music",
  "description": "Безлимитная музыка",
  "monthlyFee": 169.00,
  "categoryId": 1
}' | jq || echo

# 8. Create Subscriber
echo "8. Creating Subscriber..."
curl -s -X POST "$BASE_URL/api/v1/admin/subscribers" \
  -H "Content-Type: application/json" \
  -d '{
  "phoneNumber": "+79101234567",
  "balance": 1500.00,
  "tariffId": 1
}' | jq || echo

# 9. Change Tariff for Subscriber
echo "9. Change Tariff for Subscriber..."
curl -s -X POST "$BASE_URL/api/v1/subscribers/1/tariff/change" \
  -H "Content-Type: application/json" \
  -d '{
  "targetTariffId": 2,
  "options": {
    "minutes": "+200",
    "internet": "+20GB"
  }
}' | jq || echo

# 10. Connect Service for Subscriber
echo "10. Connect Service for Subscriber..."
curl -s -X POST "$BASE_URL/api/v1/subscribers/1/services/1/connect" \
  -H "Content-Type: application/json" \
  -d '{}' | jq || echo

echo "Database initialization completed."
