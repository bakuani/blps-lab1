package ru.urasha.callmeani.blps.service.eis.impl;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.RecordFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisOperationResult;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class DolibarrOperationAuditJcaService implements EisOperationAuditService {

    @Value("${eis.dolibarr.audit.enabled:false}")
    private boolean auditEnabled;

    @Value("${eis.dolibarr.audit.connection-factory-jndi:java:/eis/DolibarrConnectionFactory}")
    private String connectionFactoryJndi;

    @Value("${eis.dolibarr.audit.interaction-name:dolibarr.audit.operation}")
    private String interactionName;

    @Override
    public void registerOperationResult(EisOperationResult result) {
        if (!auditEnabled) {
            return;
        }

        try {
            sendViaJca(result);
        } catch (NamingException | ResourceException | RuntimeException ex) {
            log.warn(
                "Dolibarr audit send failed for requestId={}, operationType={}: {}",
                result.requestId(),
                result.operationType(),
                ex.getMessage()
            );
            log.debug("Dolibarr audit send stacktrace", ex);
        }
    }

    private void sendViaJca(EisOperationResult result) throws NamingException, ResourceException {
        Object lookedUp = new InitialContext().lookup(connectionFactoryJndi);
        if (!(lookedUp instanceof ConnectionFactory connectionFactory)) {
            throw new IllegalStateException("JNDI object is not jakarta.resource.cci.ConnectionFactory: " + connectionFactoryJndi);
        }

        Connection connection = null;
        Interaction interaction = null;
        try {
            connection = connectionFactory.getConnection();
            interaction = connection.createInteraction();

            RecordFactory recordFactory = connectionFactory.getRecordFactory();
            MappedRecord<Object, Object> payload = recordFactory.createMappedRecord("dolibarr.operation.result");
            fillPayload(payload, result);

            InteractionSpec interactionSpec = new DolibarrInteractionSpec(interactionName, InteractionSpec.SYNC_SEND_RECEIVE);
            Record output = interaction.execute(interactionSpec, payload);

            if (output instanceof MappedRecord<?, ?> outputRecord) {
                Object accepted = outputRecord.get("accepted");
                if (accepted != null && !Boolean.parseBoolean(String.valueOf(accepted))) {
                    log.warn(
                        "Dolibarr rejected audit payload for requestId={}, reason={}",
                        result.requestId(),
                        outputRecord.get("error")
                    );
                }
            }
        } finally {
            closeInteraction(interaction);
            closeConnection(connection);
        }
    }

    private void fillPayload(MappedRecord<Object, Object> payload, EisOperationResult result) {
        payload.put("operationType", result.operationType().externalCode());
        payload.put("requestId", result.requestId());
        payload.put("subscriberId", result.subscriberId());
        payload.put("amount", normalizeAmount(result.amount()));
        payload.put("status", result.status().name());
        payload.put("errorReason", result.errorReason());
        payload.put("processedAt", normalizeProcessedAt(result.processedAt()));
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String normalizeProcessedAt(OffsetDateTime processedAt) {
        return processedAt == null ? OffsetDateTime.now().toString() : processedAt.toString();
    }

    private void closeInteraction(Interaction interaction) {
        if (interaction == null) {
            return;
        }
        try {
            interaction.close();
        } catch (ResourceException ex) {
            log.debug("Failed to close JCA interaction", ex);
        }
    }

    private void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (ResourceException ex) {
            log.debug("Failed to close JCA connection", ex);
        }
    }

    private static final class DolibarrInteractionSpec implements InteractionSpec {

        @Serial
        private static final long serialVersionUID = 1L;

        private String functionName;
        private int interactionVerb;

        private DolibarrInteractionSpec(String functionName, int interactionVerb) {
            this.functionName = functionName;
            this.interactionVerb = interactionVerb;
        }

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }

        public int getInteractionVerb() {
            return interactionVerb;
        }

        public void setInteractionVerb(int interactionVerb) {
            this.interactionVerb = interactionVerb;
        }
    }
}
