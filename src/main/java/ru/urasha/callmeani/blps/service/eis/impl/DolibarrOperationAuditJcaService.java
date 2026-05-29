package ru.urasha.callmeani.blps.service.eis.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrConnection;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrConnectionFactory;
import ru.urasha.callmeani.blps.eis.dolibarr.ra.DolibarrInteraction;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisOperationResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DolibarrOperationAuditJcaService implements EisOperationAuditService {

    private final DolibarrConnectionFactory connectionFactory;

    @Value("${eis.dolibarr.audit.enabled:false}")
    private boolean auditEnabled;

    @Value("${eis.dolibarr.audit.interaction-name:dolibarr.audit.operation}")
    private String interactionName;

    @Override
    public void registerOperationResult(EisOperationResult result) {
        if (!auditEnabled) {
            return;
        }

        try {
            sendViaLocalConnector(result);
        } catch (RuntimeException ex) {
            log.warn(
                "Dolibarr audit send failed for requestId={}, operationType={}: {}",
                result.requestId(),
                result.operationType(),
                ex.getMessage()
            );
            log.debug("Dolibarr audit send stacktrace", ex);
        }
    }

    private void sendViaLocalConnector(EisOperationResult result) {
        try (DolibarrConnection connection = connectionFactory.getConnection()) {
            DolibarrInteraction interaction = connection.createInteraction();
            try {
                Map<String, Object> payload = buildPayload(result);
                DolibarrInteraction.ExecutionResult executionResult = interaction.execute(interactionName, payload);
                if (!executionResult.accepted()) {
                    log.warn(
                        "Dolibarr audit response is negative for requestId={}, statusCode={}, endpoint={}, reason={}",
                        result.requestId(),
                        executionResult.statusCode(),
                        executionResult.endpoint(),
                        executionResult.error()
                    );
                    return;
                }
                log.info(
                    "Dolibarr audit sent via local connector: requestId={}, operationType={}, statusCode={}, endpoint={}",
                    result.requestId(),
                    result.operationType(),
                    executionResult.statusCode(),
                    executionResult.endpoint()
                );
            } finally {
                interaction.close();
            }
        }
    }

    private Map<String, Object> buildPayload(EisOperationResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationType", result.operationType().externalCode());
        payload.put("requestId", result.requestId());
        payload.put("subscriberId", result.subscriberId());
        payload.put("amount", normalizeAmount(result.amount()));
        payload.put("status", result.status().name());
        payload.put("errorReason", result.errorReason());
        payload.put("processedAt", normalizeProcessedAt(result.processedAt()));
        return payload;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String normalizeProcessedAt(OffsetDateTime processedAt) {
        return processedAt == null ? OffsetDateTime.now().toString() : processedAt.toString();
    }
}
