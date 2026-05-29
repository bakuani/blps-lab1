package ru.urasha.callmeani.blps.service.tariff.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeSubmissionResponse;
import ru.urasha.callmeani.blps.api.exception.TariffChangeRequestNotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.messaging.TariffChangeRequestedMessage;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffChangeAsyncService implements TariffChangeAsyncOperations {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final TariffManagementService tariffManagementService;
    private final EisValidationService eisValidationService;
    private final EisOperationAuditService eisOperationAuditService;
    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.tariff-change-queue}")
    private String tariffChangeQueue;

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
        jmsTemplate.convertAndSend(tariffChangeQueue, new TariffChangeRequestedMessage(saved.getId()));

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
        for (TariffChangeRequest request : stuckRequests) {
            log.info("Retrying TariffChangeRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(tariffChangeQueue, new TariffChangeRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        tariffChangeRequestRepository.saveAll(stuckRequests);
        return stuckRequests.size();
    }

    @JmsListener(destination = "${app.jms.tariff-change-queue}")
    public void processTariffChange(TariffChangeRequestedMessage message) {
        TariffChangeRequest request = tariffChangeRequestRepository.findById(message.requestId())
            .orElseThrow(() -> new TariffChangeRequestNotFoundException(message.requestId()));
        BigDecimal operationAmount = BigDecimal.ZERO;

        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS || request.getStatus() == TariffChangeRequestStatus.REJECTED) {
            return;
        }

        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);

        if (!eisValidationService.allowTariffChange(request)) {
            request.setStatus(TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(ApiMessages.TARIFF_CHANGE_REJECTED_BY_EIS);
            request.setUpdatedAt(OffsetDateTime.now());
            tariffChangeRequestRepository.save(request);
            publishEisResult(request, operationAmount);
            return;
        }

        try {
            ChangeTariffResponse response = tariffManagementService.changeTariff(
                request.getSubscriberId(),
                new ChangeTariffRequest(request.getTargetTariffId(), request.getOptions())
            );
            operationAmount = sumOperationAmount(response);
            request.setStatus(response.success() ? TariffChangeRequestStatus.SUCCESS : TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(response.success() ? null : response.message());
        } catch (RuntimeException ex) {
            log.error("Tariff change request {} failed", request.getId(), ex);
            request.setStatus(resolveFailureStatus(ex, request.getAttemptCount()));
            request.setErrorMessage(ex.getMessage());
        } finally {
            request.setUpdatedAt(OffsetDateTime.now());
            tariffChangeRequestRepository.save(request);
            publishEisResult(request, operationAmount);
        }
    }

    private TariffChangeRequestStatus resolveFailureStatus(RuntimeException ex, int attemptCount) {
        if (ex instanceof NotFoundException) {
            return TariffChangeRequestStatus.REJECTED;
        }
        return attemptCount < 3 ? TariffChangeRequestStatus.RETRY : TariffChangeRequestStatus.FAILED;
    }

    private BigDecimal sumOperationAmount(ChangeTariffResponse response) {
        return response.billingTransactions()
            .stream()
            .map(tx -> tx.amount())
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void publishEisResult(TariffChangeRequest request, BigDecimal amount) {
        if (!isTerminalStatus(request.getStatus())) {
            return;
        }
        eisOperationAuditService.registerOperationResult(new EisOperationResult(
            EisOperationType.TARIFF_CHANGE_REQUESTED,
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
