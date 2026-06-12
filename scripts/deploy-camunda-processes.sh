set -e

curl -fsS \
  -X POST "http://localhost:8082/engine-rest/deployment/create" \
  -F "deployment-name=blps-camunda-processes" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  -F "feature-disable-process.bpmn=@src/main/resources/processes/feature-disable-process.bpmn;type=text/xml" \
  -F "monthly-fee-charge-process.bpmn=@src/main/resources/processes/monthly-fee-charge-process.bpmn;type=text/xml" \
  -F "monthly-fee-cycle-process.bpmn=@src/main/resources/processes/monthly-fee-cycle-process.bpmn;type=text/xml" \
  -F "tariff-change-process.bpmn=@src/main/resources/processes/tariff-change-process.bpmn;type=text/xml"

echo
echo "[camunda-deploy] SUCCESS"
