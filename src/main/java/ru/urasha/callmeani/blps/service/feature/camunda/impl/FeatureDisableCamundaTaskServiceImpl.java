package ru.urasha.callmeani.blps.service.feature.camunda.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.feature.camunda.FeatureDisableCamundaTaskService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CAN_DISABLE_FEATURE;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_AMOUNT;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_SUCCESS;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.REQUEST_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.of;

@Service
@RequiredArgsConstructor
public class FeatureDisableCamundaTaskServiceImpl implements FeatureDisableCamundaTaskService {

    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final SubscriberService subscriberService;
    private final SubscriberFeatureService subscriberFeatureService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final EisValidationService eisValidationService;
    private final EisOperationAuditService eisOperationAuditService;

    @Override
    @Transactional
    public Map<String, CamundaVariable> validateFeatureDisable(LockedExternalTask task) {
        FeatureDisableRequest request = featureRequest(task);
        if (isTerminal(request.getStatus())) {
            return of(CAN_DISABLE_FEATURE, CamundaVariable.bool(false));
        }

        markProcessing(request);
        if (!eisValidationService.allowFeatureDisable(request)) {
            rejectFeatureDisable(request, ApiMessages.FEATURE_DISABLE_REJECTED_BY_EIS);
            return of(CAN_DISABLE_FEATURE, CamundaVariable.bool(false));
        }

        Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
        boolean active = subscriberFeatureService
            .findBySubscriberIdAndFeatureIdAndStatus(request.getSubscriberId(), request.getFeatureId(), SubscriberFeatureStatus.ACTIVE)
            .isPresent();
        if (!active) {
            notificationService.createNotification(
                subscriber,
                NotificationType.SERVICE_DISABLE_ERROR,
                ApiMessages.FEATURE_NOT_ACTIVE_NOTIFICATION,
                false
            );
            rejectFeatureDisable(request, ApiMessages.FEATURE_NOT_ACTIVE_RESPONSE);
            return of(CAN_DISABLE_FEATURE, CamundaVariable.bool(false));
        }

        return of(
            CAN_DISABLE_FEATURE, CamundaVariable.bool(true),
            OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
        );
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> disableFeatureBilling(LockedExternalTask task) {
        FeatureDisableRequest request = featureRequest(task);
        Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
        String description = ApiMessages.FEATURE_DISABLE_BILLING_DESCRIPTION + " (request " + request.getId() + ")";
        if (!billingService.existsBySubscriberIdAndTypeAndDescription(
            subscriber.getId(), BillingTransactionType.SERVICE_DISABLE, description
        )) {
            billingService.createTransaction(subscriber, BillingTransactionType.SERVICE_DISABLE, BigDecimal.ZERO, description);
        }
        return of(OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString()));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> updateSubscriberFeature(LockedExternalTask task) {
        FeatureDisableRequest request = featureRequest(task);
        SubscriberFeature subscriberFeature = subscriberFeatureService
            .findBySubscriberIdAndFeatureIdAndStatus(request.getSubscriberId(), request.getFeatureId(), SubscriberFeatureStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException(ApiMessages.FEATURE_NOT_ACTIVE_RESPONSE));
        subscriberFeature.setStatus(SubscriberFeatureStatus.DISABLED);
        subscriberFeature.setDisabledAt(OffsetDateTime.now());
        subscriberFeatureService.save(subscriberFeature);

        request.setStatus(TariffChangeRequestStatus.SUCCESS);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
        return of(OPERATION_SUCCESS, CamundaVariable.bool(true));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> sendNotification(LockedExternalTask task) {
        FeatureDisableRequest request = featureRequest(task);
        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS) {
            notificationService.createNotification(
                subscriberService.getSubscriberEntity(request.getSubscriberId()),
                NotificationType.SERVICE_DISABLED,
                ApiMessages.FEATURE_DISABLED_NOTIFICATION,
                true
            );
        }
        return of();
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task) {
        FeatureDisableRequest request = featureRequest(task);
        if (isTerminal(request.getStatus())) {
            BigDecimal amount = decimal(task.stringVariable(OPERATION_AMOUNT));
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
        return of();
    }

    @Override
    @Transactional
    public void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft) {
        Long requestId = task.longVariable(REQUEST_ID);
        if (requestId == null) {
            return;
        }
        featureDisableRequestRepository.findById(requestId)
            .ifPresent(request -> markFailed(request, failureStatus(exception, retriesLeft), exception));
    }

    private void markProcessing(FeatureDisableRequest request) {
        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
    }

    private void rejectFeatureDisable(FeatureDisableRequest request, String message) {
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
    }

    private void markFailed(FeatureDisableRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
    }

    private FeatureDisableRequest featureRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return featureDisableRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("FeatureDisableRequest not found: " + id));
    }

    private boolean isTerminal(TariffChangeRequestStatus status) {
        return status == TariffChangeRequestStatus.SUCCESS
            || status == TariffChangeRequestStatus.REJECTED
            || status == TariffChangeRequestStatus.FAILED;
    }

    private TariffChangeRequestStatus failureStatus(RuntimeException exception, int retriesLeft) {
        return exception instanceof NotFoundException
            ? TariffChangeRequestStatus.REJECTED
            : retriesLeft <= 0 ? TariffChangeRequestStatus.FAILED : TariffChangeRequestStatus.RETRY;
    }

    private BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
