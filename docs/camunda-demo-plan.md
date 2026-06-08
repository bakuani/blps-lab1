# Camunda deployment and demo notes

## Runtime layout

- WildFly with the BLPS WAR runs on helios.
- Camunda 7 Run runs locally in Docker Compose on `127.0.0.1:8082`.
- The WAR on helios reaches Camunda through the SSH reverse tunnel:
  `127.0.0.1:18080 -> local 127.0.0.1:8082`.
- RabbitMQ and Dolibarr remain in Docker Compose and are still exposed to helios through the existing reverse tunnels.

## Startup order

1. Start local infrastructure:
   ```sh
   docker compose up -d postgres rabbitmq dolibarr-db dolibarr camunda-db camunda
   ```
2. Deploy the executable BPMN model to Camunda:
   ```sh
   ./scripts/deploy-camunda-processes.sh
   ```
3. Open the SSH tunnel:
   ```sh
   ./scripts/open-tunnel.sh
   ```
4. Start WildFly on helios:
   ```sh
   ./scripts/helios-start.sh
   ```
5. Build and deploy the WAR with the existing WildFly scripts.

## Useful URLs

- BLPS API through helios tunnel: `http://127.0.0.1:8180`
- Local Camunda Tasklist: `http://127.0.0.1:8082/camunda`
- Local Camunda REST API: `http://127.0.0.1:8082/engine-rest`
- Camunda REST API as seen from helios WAR: `http://127.0.0.1:18080/engine-rest`

## Demo focus

- `POST /api/v1/subscribers/{subscriberId}/tariff/change` starts `TariffChangeProcess`.
- `POST /api/v1/subscribers/{subscriberId}/features/{featureId}/disable` starts `FeatureDisableProcess`.
- `MonthlyFeeCycleProcess` is started by a Camunda timer and creates `MonthlyFeeChargeProcess` instances.
- Business routing, retries, and periodic execution are visible in Camunda Cockpit/Tasklist.
- JPA transactions, Dolibarr calls, notification creation, and EIS audit are executed by external task workers inside the WildFly application.
