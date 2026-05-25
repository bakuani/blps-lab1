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
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.messaging.FeatureDisableRequestedMessage;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.feature.FeatureManagementService;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureDisableAsyncService {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final FeatureManagementService featureManagementService;
    private final EisValidationService eisValidationService;
    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.feature-disable-queue}")
    private String featureDisableQueue;

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

    @Transactional
    @JmsListener(destination = "${app.jms.feature-disable-queue}")
    public void processFeatureDisable(FeatureDisableRequestedMessage message) {
        FeatureDisableRequest request = featureDisableRequestRepository.findById(message.requestId())
            .orElseThrow(() -> new FeatureDisableRequestNotFoundException(message.requestId()));

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
            return;
        }

        try {
            DisableFeatureResponse response = featureManagementService.disableFeature(request.getSubscriberId(), request.getFeatureId());
            request.setStatus(response.success() ? TariffChangeRequestStatus.SUCCESS : TariffChangeRequestStatus.REJECTED);
            request.setErrorMessage(response.success() ? null : response.message());
        } catch (RuntimeException ex) {
            log.error("Feature disable request {} failed", request.getId(), ex);
            request.setStatus(TariffChangeRequestStatus.FAILED);
            request.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            request.setUpdatedAt(OffsetDateTime.now());
            featureDisableRequestRepository.save(request);
        }
    }
}
