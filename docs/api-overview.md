# BLPS API Overview

Проект реализует BPMN-процесс управления тарифом и услугами в личном кабинете абонента.

## Технологии
- Spring Boot 4
- Spring Web
- Spring Data JPA
- PostgreSQL

## REST API

### Тарифы
- GET `/api/v1/subscribers/{subscriberId}/tariff` - текущий тариф и баланс абонента
- GET `/api/v1/tariffs/categories` - категории тарифов
- GET `/api/v1/tariffs?categoryId=&query=` - поиск тарифов
- GET `/api/v1/tariffs/{tariffId}` - карточка тарифа и опции
- POST `/api/v1/subscribers/{subscriberId}/tariff/change` - смена тарифа

### Услуги
- GET `/api/v1/subscribers/{subscriberId}/services?categoryId=&query=` - подключенные услуги абонента
- GET `/api/v1/services/categories` - категории услуг
- GET `/api/v1/services/{serviceId}` - карточка услуги
- POST `/api/v1/subscribers/{subscriberId}/services/{serviceId}/disable` - отключить услугу

## Запуск

Перед запуском поднимите PostgreSQL и создайте БД `blps`.

Пример переменных окружения:
- `DB_URL=jdbc:postgresql://localhost:5432/blps`
- `DB_USER=postgres`
- `DB_PASSWORD=postgres`

Запуск:

```bash
./gradlew bootRun
```

После старта автоматически создаются демо-данные (1 абонент, тарифы, услуги).

## Insomnia

Экспорт коллекции запросов находится в файле:
- `docs/insomnia-export.json`
