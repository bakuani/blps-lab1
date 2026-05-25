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
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffChangeAsyncService {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final TariffManagementService tariffManagementService;
    private final EisValidationService eisValidationService;
    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.tariff-change-queue}")
    private String tariffChangeQueue;

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

    @JmsListener(destination = "${app.jms.tariff-change-queue}")
    public void processTariffChange(TariffChangeRequestedMessage message) {
        TariffChangeRequest request = tariffChangeRequestRepository.findById(message.requestId())
            .orElseThrow(() -> new TariffChangeRequestNotFoundException(message.requestId()));

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
            return;
        }

        try {
            ChangeTariffResponse response = tariffManagementService.changeTariff(
                request.getSubscriberId(),
                new ChangeTariffRequest(request.getTargetTariffId(), request.getOptions())
            );
            request.setStatus(response.success() ? TariffChangeRequestStatus.SUCCESS : TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(response.success() ? null : response.message());
        } catch (RuntimeException ex) {
            log.error("Tariff change request {} failed", request.getId(), ex);
            request.setStatus(resolveFailureStatus(ex, request.getAttemptCount()));
            request.setErrorMessage(ex.getMessage());
        } finally {
            request.setUpdatedAt(OffsetDateTime.now());
            tariffChangeRequestRepository.save(request);
        }
    }

    private TariffChangeRequestStatus resolveFailureStatus(RuntimeException ex, int attemptCount) {
        if (ex instanceof NotFoundException) {
            return TariffChangeRequestStatus.REJECTED;
        }
        return attemptCount < 3 ? TariffChangeRequestStatus.RETRY : TariffChangeRequestStatus.FAILED;
    }
}
