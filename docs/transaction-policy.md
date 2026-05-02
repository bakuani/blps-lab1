# Transaction Policy

## Programmatic transaction management

Application uses `TransactionTemplate` and `JtaTransactionManager`.

Programmatic transaction boundaries are implemented only for two agreed use-cases:

1. `changeTariff(subscriberId, request)`
2. `disableFeature(subscriberId, featureId)`

## Use-case: changeTariff

Single atomic transaction includes:

- `subscriber` update (balance and current tariff)
- `billing_transaction` inserts
- `notification_event` insert

On any runtime failure the full transaction is rolled back.

## Use-case: disableFeature

Single atomic transaction includes:

- `subscriber_feature` update (`status=DISABLED`, `disabled_at`)
- `billing_transaction` insert
- `notification_event` insert

On any runtime failure the full transaction is rolled back.

