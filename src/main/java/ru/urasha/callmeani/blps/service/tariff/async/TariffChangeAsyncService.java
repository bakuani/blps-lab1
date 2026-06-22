package ru.urasha.callmeani.blps.service.tariff.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeSubmissionResponse;
import ru.urasha.callmeani.blps.api.exception.TariffChangeRequestNotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.camunda.client.CamundaRestClient;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_OPERATION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_REQUEST_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_INSTANCE_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_KEY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CORRELATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffChangeAsyncService implements TariffChangeAsyncOperations {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final CamundaRestClient camundaRestClient;

    @Override
    @Transactional
    public TariffChangeSubmissionResponse submitTariffChange(Long subscriberId, ChangeTariffRequest request) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "business_process_submitted",
            BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_TARIFF_CHANGE,
            PROCESS_KEY, CamundaProcessConstants.TARIFF_CHANGE_PROCESS,
            SUBSCRIBER_ID, subscriberId
        )) {
            TariffChangeRequest requestEntity = new TariffChangeRequest();
            requestEntity.setSubscriberId(subscriberId);
            requestEntity.setTargetTariffId(request.targetTariffId());
            requestEntity.setOptions(request.options() == null ? Map.of() : request.options());
            requestEntity.setStatus(TariffChangeRequestStatus.PENDING);
            requestEntity.setAttemptCount(0);
            requestEntity.setCreatedAt(OffsetDateTime.now());
            requestEntity.setUpdatedAt(OffsetDateTime.now());

            TariffChangeRequest saved = tariffChangeRequestRepository.save(requestEntity);
            try (LoggingContext requestContext = LoggingContext.open(
                BUSINESS_REQUEST_ID, saved.getId(),
                EVENT_OUTCOME, "success"
            )) {
                log.info("Tariff change request accepted");
                startProcess(saved);
            }

            return new TariffChangeSubmissionResponse(
                saved.getId(),
                saved.getStatus(),
                ApiMessages.TARIFF_CHANGE_REQUEST_ACCEPTED
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TariffChangeRequestStatusResponse getStatus(Long subscriberId, Long requestId) {
        TariffChangeRequest request = tariffChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new TariffChangeRequestNotFoundException(requestId));
        if (!request.getSubscriberId().equals(subscriberId)) {
            throw new TariffChangeRequestNotFoundException(requestId);
        }

        return new TariffChangeRequestStatusResponse(
            request.getId(),
            request.getStatus(),
            request.getErrorMessage(),
            request.getAttemptCount(),
            request.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses) {
        List<TariffChangeRequest> stuckRequests = tariffChangeRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        int restarted = 0;
        for (TariffChangeRequest request : stuckRequests) {
            if (request.getProcessInstanceId() != null) {
                continue;
            }
            try (LoggingContext ignored = LoggingContext.open(
                EVENT_CATEGORY, "process",
                EVENT_ACTION, "business_process_retry",
                BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_TARIFF_CHANGE,
                BUSINESS_REQUEST_ID, request.getId(),
                SUBSCRIBER_ID, request.getSubscriberId()
            )) {
                log.warn("Restarting Camunda process for stuck tariff change request");
                startProcess(request);
            }
            restarted++;
        }
        return restarted;
    }

    private void startProcess(TariffChangeRequest request) {
        Map<String, CamundaVariable> variables = processVariables(request);
        String processInstanceId = camundaRestClient.startProcess(
            CamundaProcessConstants.TARIFF_CHANGE_PROCESS,
            "tariff-change-" + request.getId(),
            variables
        );
        request.setProcessInstanceId(processInstanceId);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
        camundaRestClient.completeFirstTask(processInstanceId, variables);
        try (LoggingContext ignored = LoggingContext.open(
            PROCESS_INSTANCE_ID, processInstanceId,
            EVENT_OUTCOME, "success"
        )) {
            log.info("Tariff change Camunda process started");
        }
    }

    private Map<String, CamundaVariable> processVariables(TariffChangeRequest request) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        variables.put(CORRELATION_ID, CamundaVariable.string(LoggingContext.getOrCreateCorrelationId()));
        variables.put("operationType", CamundaVariable.string(CamundaProcessConstants.OPERATION_TARIFF_CHANGE));
        variables.put("requestId", CamundaVariable.longValue(request.getId()));
        variables.put("subscriberId", CamundaVariable.longValue(request.getSubscriberId()));
        variables.put("targetTariffId", CamundaVariable.longValue(request.getTargetTariffId()));
        variables.put("options", CamundaVariable.string(request.getOptions().toString()));
        variables.put("operationAmount", CamundaVariable.string("0"));
        return variables;
    }
}
