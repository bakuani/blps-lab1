package ru.urasha.callmeani.blps.service.feature.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.feature.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableSubmissionResponse;
import ru.urasha.callmeani.blps.api.exception.FeatureDisableRequestNotFoundException;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.messaging.FeatureDisableRequestedMessage;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.feature.FeatureManagementService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureDisableAsyncService implements FeatureDisableAsyncOperations {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final FeatureManagementService featureManagementService;
    private final EisValidationService eisValidationService;
    private final EisOperationAuditService eisOperationAuditService;
    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.feature-disable-queue}")
    private String featureDisableQueue;

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
        jmsTemplate.convertAndSend(featureDisableQueue, new FeatureDisableRequestedMessage(saved.getId()));

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
        for (FeatureDisableRequest request : stuckRequests) {
            log.info("Retrying FeatureDisableRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(featureDisableQueue, new FeatureDisableRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        featureDisableRequestRepository.saveAll(stuckRequests);
        return stuckRequests.size();
    }

    @JmsListener(destination = "${app.jms.feature-disable-queue}")
    public void processFeatureDisable(FeatureDisableRequestedMessage message) {
        FeatureDisableRequest request = featureDisableRequestRepository.findById(message.requestId())
            .orElseThrow(() -> new FeatureDisableRequestNotFoundException(message.requestId()));
        BigDecimal operationAmount = BigDecimal.ZERO;

        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS || request.getStatus() == TariffChangeRequestStatus.REJECTED) {
            return;
        }

        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);

        if (!eisValidationService.allowFeatureDisable(request)) {
            request.setStatus(TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(ApiMessages.FEATURE_DISABLE_REJECTED_BY_EIS);
            request.setUpdatedAt(OffsetDateTime.now());
            featureDisableRequestRepository.save(request);
            publishEisResult(request, operationAmount);
            return;
        }

        try {
            DisableFeatureResponse response = featureManagementService.disableFeature(request.getSubscriberId(), request.getFeatureId());
            operationAmount = sumOperationAmount(response);
            request.setStatus(response.success() ? TariffChangeRequestStatus.SUCCESS : TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(response.success() ? null : response.message());
        } catch (RuntimeException ex) {
            log.error("Feature disable request {} failed", request.getId(), ex);
            request.setStatus(resolveFailureStatus(ex, request.getAttemptCount()));
            request.setErrorMessage(ex.getMessage());
        } finally {
            request.setUpdatedAt(OffsetDateTime.now());
            featureDisableRequestRepository.save(request);
            publishEisResult(request, operationAmount);
        }
    }

    private TariffChangeRequestStatus resolveFailureStatus(RuntimeException ex, int attemptCount) {
        if (ex instanceof NotFoundException) {
            return TariffChangeRequestStatus.REJECTED;
        }
        return attemptCount < 3 ? TariffChangeRequestStatus.RETRY : TariffChangeRequestStatus.FAILED;
    }

    private BigDecimal sumOperationAmount(DisableFeatureResponse response) {
        return response.billingTransactions()
            .stream()
            .map(tx -> tx.amount())
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void publishEisResult(FeatureDisableRequest request, BigDecimal amount) {
        if (!isTerminalStatus(request.getStatus())) {
            return;
        }
        eisOperationAuditService.registerOperationResult(new EisOperationResult(
            EisOperationType.FEATURE_DISABLE_REQUESTED,
            request.getId(),
            request.getSubscriberId(),
            amount,
            request.getStatus(),
            request.getErrorMessage(),
            OffsetDateTime.now()
        ));
    }

    private boolean isTerminalStatus(TariffChangeRequestStatus status) {
        return status == TariffChangeRequestStatus.SUCCESS
            || status == TariffChangeRequestStatus.REJECTED
            || status == TariffChangeRequestStatus.FAILED;
    }
}
