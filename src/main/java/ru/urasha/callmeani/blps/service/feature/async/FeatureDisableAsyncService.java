package ru.urasha.callmeani.blps.service.feature.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableSubmissionResponse;
import ru.urasha.callmeani.blps.api.exception.FeatureDisableRequestNotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
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
public class FeatureDisableAsyncService implements FeatureDisableAsyncOperations {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final CamundaRestClient camundaRestClient;

    @Override
    @Transactional
    public FeatureDisableSubmissionResponse submitFeatureDisable(Long subscriberId, Long featureId) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "business_process_submitted",
            BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_FEATURE_DISABLE,
            PROCESS_KEY, CamundaProcessConstants.FEATURE_DISABLE_PROCESS,
            SUBSCRIBER_ID, subscriberId
        )) {
            FeatureDisableRequest request = new FeatureDisableRequest();
            request.setSubscriberId(subscriberId);
            request.setFeatureId(featureId);
            request.setStatus(BusinessRequestStatus.PENDING);
            request.setAttemptCount(0);

            FeatureDisableRequest saved = featureDisableRequestRepository.save(request);
            try (LoggingContext requestContext = LoggingContext.open(
                BUSINESS_REQUEST_ID, saved.getId(),
                EVENT_OUTCOME, "success"
            )) {
                log.info("Feature disable request accepted");
                startProcess(saved);
            }

            return new FeatureDisableSubmissionResponse(
                saved.getId(),
                saved.getStatus(),
                ApiMessages.FEATURE_DISABLE_REQUEST_ACCEPTED
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureDisableRequestStatusResponse getStatus(Long subscriberId, Long requestId) {
        FeatureDisableRequest request = featureDisableRequestRepository.findById(requestId)
            .orElseThrow(() -> new FeatureDisableRequestNotFoundException(requestId));
        if (!request.getSubscriberId().equals(subscriberId)) {
            throw new FeatureDisableRequestNotFoundException(requestId);
        }

        return new FeatureDisableRequestStatusResponse(
            request.getId(),
            request.getStatus(),
            request.getErrorMessage(),
            request.getAttemptCount(),
            request.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public int retryStuckOperations(OffsetDateTime threshold, List<BusinessRequestStatus> targetStatuses) {
        List<FeatureDisableRequest> stuckRequests = featureDisableRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        int restarted = 0;
        for (FeatureDisableRequest request : stuckRequests) {
            if (request.getProcessInstanceId() != null) {
                continue;
            }
            try (LoggingContext ignored = LoggingContext.open(
                EVENT_CATEGORY, "process",
                EVENT_ACTION, "business_process_retry",
                BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_FEATURE_DISABLE,
                BUSINESS_REQUEST_ID, request.getId(),
                SUBSCRIBER_ID, request.getSubscriberId()
            )) {
                log.warn("Restarting Camunda process for stuck feature disable request");
                startProcess(request);
            }
            restarted++;
        }
        return restarted;
    }

    private void startProcess(FeatureDisableRequest request) {
        Map<String, CamundaVariable> variables = processVariables(request);
        String processInstanceId = camundaRestClient.startProcess(
            CamundaProcessConstants.FEATURE_DISABLE_PROCESS,
            "feature-disable-" + request.getId(),
            variables
        );
        request.setProcessInstanceId(processInstanceId);
        featureDisableRequestRepository.save(request);
        camundaRestClient.completeFirstTask(processInstanceId, variables);
        try (LoggingContext ignored = LoggingContext.open(
            PROCESS_INSTANCE_ID, processInstanceId,
            EVENT_OUTCOME, "success"
        )) {
            log.info("Feature disable Camunda process started");
        }
    }

    private Map<String, CamundaVariable> processVariables(FeatureDisableRequest request) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        variables.put(CORRELATION_ID, CamundaVariable.string(LoggingContext.getOrCreateCorrelationId()));
        variables.put("operationType", CamundaVariable.string(CamundaProcessConstants.OPERATION_FEATURE_DISABLE));
        variables.put("requestId", CamundaVariable.longValue(request.getId()));
        variables.put("subscriberId", CamundaVariable.longValue(request.getSubscriberId()));
        variables.put("featureId", CamundaVariable.longValue(request.getFeatureId()));
        variables.put("operationAmount", CamundaVariable.string("0"));
        return variables;
    }
}
