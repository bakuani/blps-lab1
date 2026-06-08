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
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.service.camunda.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.camunda.CamundaRestClient;
import ru.urasha.callmeani.blps.service.camunda.CamundaVariable;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureDisableAsyncService implements FeatureDisableAsyncOperations {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final CamundaRestClient camundaRestClient;

    @Override
    @Transactional
    public FeatureDisableSubmissionResponse submitFeatureDisable(Long subscriberId, Long featureId) {
        FeatureDisableRequest request = new FeatureDisableRequest();
        request.setSubscriberId(subscriberId);
        request.setFeatureId(featureId);
        request.setStatus(TariffChangeRequestStatus.PENDING);
        request.setAttemptCount(0);
        request.setCreatedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());

        FeatureDisableRequest saved = featureDisableRequestRepository.save(request);
        startProcess(saved);

        return new FeatureDisableSubmissionResponse(
            saved.getId(),
            saved.getStatus(),
            ApiMessages.FEATURE_DISABLE_REQUEST_ACCEPTED
        );
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
    public int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses) {
        List<FeatureDisableRequest> stuckRequests = featureDisableRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        int restarted = 0;
        for (FeatureDisableRequest request : stuckRequests) {
            if (request.getProcessInstanceId() != null) {
                continue;
            }
            log.info("Starting Camunda process for stuck FeatureDisableRequest with id {}", request.getId());
            startProcess(request);
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
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
        camundaRestClient.completeFirstTask(processInstanceId, variables);
    }

    private Map<String, CamundaVariable> processVariables(FeatureDisableRequest request) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        variables.put("operationType", CamundaVariable.string(CamundaProcessConstants.OPERATION_FEATURE_DISABLE));
        variables.put("requestId", CamundaVariable.longValue(request.getId()));
        variables.put("subscriberId", CamundaVariable.longValue(request.getSubscriberId()));
        variables.put("featureId", CamundaVariable.longValue(request.getFeatureId()));
        variables.put("operationAmount", CamundaVariable.string("0"));
        return variables;
    }
}
