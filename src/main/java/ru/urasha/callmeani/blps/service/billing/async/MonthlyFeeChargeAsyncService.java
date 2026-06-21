package ru.urasha.callmeani.blps.service.billing.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.billing.MonthlyFeeChargeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.exception.MonthlyFeeChargeRequestNotFoundException;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.camunda.client.CamundaRestClient;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_OPERATION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_REQUEST_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_INSTANCE_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CORRELATION_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyFeeChargeAsyncService implements MonthlyFeeChargeAsyncOperations {

    private final SubscriberService subscriberService;
    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final CamundaRestClient camundaRestClient;

    @Value("${app.scheduler.monthly-fee-cycle-pattern}")
    private String cyclePattern;

    @Override
    @Transactional
    public int enqueueCurrentCycleCharges() {
        String billingPeriod = DateTimeFormatter.ofPattern(cyclePattern).format(OffsetDateTime.now());
        List<Subscriber> subscribers = subscriberService.findWithCurrentTariff();
        int created = 0;
        for (Subscriber subscriber : subscribers) {
            if (monthlyFeeChargeRequestRepository.existsBySubscriberIdAndBillingPeriod(subscriber.getId(), billingPeriod)) {
                continue;
            }
            MonthlyFeeChargeRequest request = new MonthlyFeeChargeRequest();
            request.setSubscriberId(subscriber.getId());
            request.setBillingPeriod(billingPeriod);
            request.setStatus(TariffChangeRequestStatus.PENDING);
            request.setAttemptCount(0);
            request.setCreatedAt(OffsetDateTime.now());
            request.setUpdatedAt(OffsetDateTime.now());
            MonthlyFeeChargeRequest saved = monthlyFeeChargeRequestRepository.save(request);
            startProcess(saved);
            created++;
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyFeeChargeRequestStatusResponse getStatus(Long subscriberId, Long requestId) {
        MonthlyFeeChargeRequest request = monthlyFeeChargeRequestRepository.findById(requestId)
            .orElseThrow(() -> new MonthlyFeeChargeRequestNotFoundException(requestId));
        if (!request.getSubscriberId().equals(subscriberId)) {
            throw new MonthlyFeeChargeRequestNotFoundException(requestId);
        }
        return toStatusResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyFeeChargeRequestStatusResponse> getRecentStatuses(Long subscriberId) {
        return monthlyFeeChargeRequestRepository.findTop10BySubscriberIdOrderByCreatedAtDesc(subscriberId)
            .stream()
            .map(this::toStatusResponse)
            .toList();
    }

    @Override
    @Transactional
    public int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses) {
        List<MonthlyFeeChargeRequest> stuckRequests = monthlyFeeChargeRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        int restarted = 0;
        for (MonthlyFeeChargeRequest request : stuckRequests) {
            if (request.getProcessInstanceId() != null) {
                continue;
            }
            try (LoggingContext ignored = LoggingContext.open(
                EVENT_CATEGORY, "process",
                EVENT_ACTION, "business_process_retry",
                BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_MONTHLY_FEE,
                BUSINESS_REQUEST_ID, request.getId(),
                SUBSCRIBER_ID, request.getSubscriberId()
            )) {
                log.warn("Restarting Camunda process for stuck monthly fee request");
                startProcess(request);
            }
            restarted++;
        }
        return restarted;
    }

    private void startProcess(MonthlyFeeChargeRequest request) {
        Map<String, CamundaVariable> variables = processVariables(request);
        String processInstanceId = camundaRestClient.startProcess(
            CamundaProcessConstants.MONTHLY_FEE_CHARGE_PROCESS,
            "monthly-fee-" + request.getId(),
            variables
        );
        request.setProcessInstanceId(processInstanceId);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "business_process_started",
            EVENT_OUTCOME, "success",
            BUSINESS_OPERATION, CamundaProcessConstants.OPERATION_MONTHLY_FEE,
            BUSINESS_REQUEST_ID, request.getId(),
            SUBSCRIBER_ID, request.getSubscriberId(),
            PROCESS_INSTANCE_ID, processInstanceId
        )) {
            log.info("Monthly fee Camunda process started");
        }
    }

    private Map<String, CamundaVariable> processVariables(MonthlyFeeChargeRequest request) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        variables.put(CORRELATION_ID, CamundaVariable.string(LoggingContext.getOrCreateCorrelationId()));
        variables.put("operationType", CamundaVariable.string(CamundaProcessConstants.OPERATION_MONTHLY_FEE));
        variables.put("requestId", CamundaVariable.longValue(request.getId()));
        variables.put("subscriberId", CamundaVariable.longValue(request.getSubscriberId()));
        variables.put("billingPeriod", CamundaVariable.string(request.getBillingPeriod()));
        variables.put("operationAmount", CamundaVariable.string("0"));
        return variables;
    }

    private MonthlyFeeChargeRequestStatusResponse toStatusResponse(MonthlyFeeChargeRequest request) {
        return new MonthlyFeeChargeRequestStatusResponse(
            request.getId(),
            request.getBillingPeriod(),
            request.getStatus(),
            request.getErrorMessage(),
            request.getAttemptCount(),
            request.getDolibarrThirdPartyId(),
            request.getDolibarrInvoiceId(),
            request.getDolibarrInvoiceRef(),
            request.getUpdatedAt()
        );
    }
}
