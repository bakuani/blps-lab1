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
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
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
public class TariffChangeAsyncService implements TariffChangeAsyncOperations {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final CamundaRestClient camundaRestClient;

    @Override
    @Transactional
    public TariffChangeSubmissionResponse submitTariffChange(Long subscriberId, ChangeTariffRequest request) {
        TariffChangeRequest requestEntity = new TariffChangeRequest();
        requestEntity.setSubscriberId(subscriberId);
        requestEntity.setTargetTariffId(request.targetTariffId());
        requestEntity.setOptions(request.options() == null ? Map.of() : request.options());
        requestEntity.setStatus(TariffChangeRequestStatus.PENDING);
        requestEntity.setAttemptCount(0);
        requestEntity.setCreatedAt(OffsetDateTime.now());
        requestEntity.setUpdatedAt(OffsetDateTime.now());

        TariffChangeRequest saved = tariffChangeRequestRepository.save(requestEntity);
        startProcess(saved);

        return new TariffChangeSubmissionResponse(
            saved.getId(),
            saved.getStatus(),
            ApiMessages.TARIFF_CHANGE_REQUEST_ACCEPTED
        );
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
            log.info("Starting Camunda process for stuck TariffChangeRequest with id {}", request.getId());
            startProcess(request);
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
    }

    private Map<String, CamundaVariable> processVariables(TariffChangeRequest request) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        variables.put("operationType", CamundaVariable.string(CamundaProcessConstants.OPERATION_TARIFF_CHANGE));
        variables.put("requestId", CamundaVariable.longValue(request.getId()));
        variables.put("subscriberId", CamundaVariable.longValue(request.getSubscriberId()));
        variables.put("targetTariffId", CamundaVariable.longValue(request.getTargetTariffId()));
        variables.put("options", CamundaVariable.string(request.getOptions().toString()));
        variables.put("operationAmount", CamundaVariable.string("0"));
        return variables;
    }
}
