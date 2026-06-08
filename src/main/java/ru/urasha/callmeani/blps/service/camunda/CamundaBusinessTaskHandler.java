package ru.urasha.callmeani.blps.service.camunda;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.billing.async.MonthlyFeeChargeAsyncOperations;
import ru.urasha.callmeani.blps.service.eis.DolibarrInvoiceService;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaBusinessTaskHandler implements CamundaExternalTaskHandler {

    private static final String REQUEST_ID = "requestId";
    private static final String OPERATION_TYPE = "operationType";
    private static final String OPERATION_AMOUNT = "operationAmount";

    private static final String TARIFF_CHANGE = CamundaProcessConstants.OPERATION_TARIFF_CHANGE;
    private static final String FEATURE_DISABLE = CamundaProcessConstants.OPERATION_FEATURE_DISABLE;
    private static final String MONTHLY_FEE = CamundaProcessConstants.OPERATION_MONTHLY_FEE;

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final SubscriberService subscriberService;
    private final SubscriberFeatureService subscriberFeatureService;
    private final TariffService tariffService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final EisValidationService eisValidationService;
    private final EisOperationAuditService eisOperationAuditService;
    private final DolibarrInvoiceService dolibarrInvoiceService;
    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;
    @Qualifier("businessTransactionTemplate")
    private final TransactionTemplate businessTransactionTemplate;

    @Override
    public Set<String> topics() {
        return Set.of(
            CamundaProcessConstants.VALIDATE_TARIFF_CHANGE,
            CamundaProcessConstants.CHARGE_SWITCH_FEE,
            CamundaProcessConstants.CHARGE_NEW_MONTHLY_FEE,
            CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF,
            CamundaProcessConstants.VALIDATE_FEATURE_DISABLE,
            CamundaProcessConstants.DISABLE_FEATURE_BILLING,
            CamundaProcessConstants.UPDATE_SUBSCRIBER_FEATURE,
            CamundaProcessConstants.CREATE_MONTHLY_FEE_REQUESTS,
            CamundaProcessConstants.CREATE_DOLIBARR_INVOICE,
            CamundaProcessConstants.CHARGE_MONTHLY_FEE,
            CamundaProcessConstants.SYNC_DOLIBARR_INVOICE,
            CamundaProcessConstants.SEND_NOTIFICATION,
            CamundaProcessConstants.PUBLISH_EIS_AUDIT
        );
    }

    @Override
    public Map<String, CamundaVariable> handle(LockedExternalTask task) {
        return switch (task.topicName()) {
            case CamundaProcessConstants.VALIDATE_TARIFF_CHANGE -> validateTariffChange(task);
            case CamundaProcessConstants.CHARGE_SWITCH_FEE -> chargeSwitchFee(task);
            case CamundaProcessConstants.CHARGE_NEW_MONTHLY_FEE -> chargeNewMonthlyFee(task);
            case CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF -> updateSubscriberTariff(task);
            case CamundaProcessConstants.VALIDATE_FEATURE_DISABLE -> validateFeatureDisable(task);
            case CamundaProcessConstants.DISABLE_FEATURE_BILLING -> disableFeatureBilling(task);
            case CamundaProcessConstants.UPDATE_SUBSCRIBER_FEATURE -> updateSubscriberFeature(task);
            case CamundaProcessConstants.CREATE_MONTHLY_FEE_REQUESTS -> createMonthlyFeeRequests();
            case CamundaProcessConstants.CREATE_DOLIBARR_INVOICE -> createDolibarrInvoice(task);
            case CamundaProcessConstants.CHARGE_MONTHLY_FEE -> chargeMonthlyFee(task);
            case CamundaProcessConstants.SYNC_DOLIBARR_INVOICE -> syncDolibarrInvoice(task);
            case CamundaProcessConstants.SEND_NOTIFICATION -> sendNotification(task);
            case CamundaProcessConstants.PUBLISH_EIS_AUDIT -> publishEisAudit(task);
            default -> throw new IllegalArgumentException("Unsupported Camunda topic: " + task.topicName());
        };
    }

    @Override
    public void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft) {
        Long requestId = task.longVariable(REQUEST_ID);
        if (requestId == null) {
            return;
        }
        TariffChangeRequestStatus status = exception instanceof NotFoundException
            ? TariffChangeRequestStatus.REJECTED
            : retriesLeft <= 0 ? TariffChangeRequestStatus.FAILED : TariffChangeRequestStatus.RETRY;
        String operationType = task.stringVariable(OPERATION_TYPE);
        if (TARIFF_CHANGE.equals(operationType)) {
            tariffChangeRequestRepository.findById(requestId).ifPresent(request -> markFailed(request, status, exception));
        } else if (FEATURE_DISABLE.equals(operationType)) {
            featureDisableRequestRepository.findById(requestId).ifPresent(request -> markFailed(request, status, exception));
        } else if (MONTHLY_FEE.equals(operationType)) {
            monthlyFeeChargeRequestRepository.findById(requestId).ifPresent(request -> markFailed(request, status, exception));
        }
    }

    private Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            TariffChangeRequest request = tariffRequest(task);
            if (isTerminal(request.getStatus())) {
                return variables("canChangeTariff", CamundaVariable.bool(false));
            }

            request.setStatus(TariffChangeRequestStatus.PROCESSING);
            request.setAttemptCount(request.getAttemptCount() + 1);
            request.setErrorMessage(null);
            request.setUpdatedAt(OffsetDateTime.now());
            tariffChangeRequestRepository.save(request);

            if (!eisValidationService.allowTariffChange(request)) {
                rejectTariffChange(request, ApiMessages.TARIFF_CHANGE_REJECTED_BY_EIS);
                return variables("canChangeTariff", CamundaVariable.bool(false));
            }

            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            Tariff targetTariff = tariffService.getTariffEntity(request.getTargetTariffId());
            Tariff currentTariff = subscriber.getCurrentTariff();

            if (currentTariff != null && currentTariff.getId().equals(targetTariff.getId())) {
                notificationService.createNotification(
                    subscriber,
                    NotificationType.TARIFF_CHANGE_ERROR,
                    ApiMessages.TARIFF_ALREADY_SELECTED_NOTIFICATION,
                    false
                );
                rejectTariffChange(request, ApiMessages.TARIFF_ALREADY_SELECTED_RESPONSE);
                return variables("canChangeTariff", CamundaVariable.bool(false));
            }

            BigDecimal switchFee = targetTariff.getSwitchFee();
            BigDecimal monthlyFee = targetTariff.getMonthlyFee();
            BigDecimal total = switchFee.add(monthlyFee);
            if (subscriber.getBalance().compareTo(total) < 0) {
                notificationService.createNotification(
                    subscriber,
                    NotificationType.TARIFF_CHANGE_ERROR,
                    ApiMessages.TARIFF_INSUFFICIENT_FUNDS_NOTIFICATION,
                    false
                );
                rejectTariffChange(request, ApiMessages.TARIFF_INSUFFICIENT_FUNDS_RESPONSE);
                return variables("canChangeTariff", CamundaVariable.bool(false));
            }

            return variables(
                "canChangeTariff", CamundaVariable.bool(true),
                "hasSwitchFee", CamundaVariable.bool(switchFee.compareTo(BigDecimal.ZERO) > 0),
                OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
            );
        });
    }

    private Map<String, CamundaVariable> chargeSwitchFee(LockedExternalTask task) {
        return chargeTariffAmount(task, BillingTransactionType.TARIFF_SWITCH_FEE, ApiMessages.TARIFF_SWITCH_FEE_DESCRIPTION);
    }

    private Map<String, CamundaVariable> chargeNewMonthlyFee(LockedExternalTask task) {
        return chargeTariffAmount(task, BillingTransactionType.MONTHLY_TARIFF_FEE, ApiMessages.TARIFF_MONTHLY_FEE_DESCRIPTION);
    }

    private Map<String, CamundaVariable> chargeTariffAmount(
        LockedExternalTask task,
        BillingTransactionType transactionType,
        String descriptionPrefix
    ) {
        return businessTransactionTemplate.execute(status -> {
            TariffChangeRequest request = tariffRequest(task);
            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            Tariff targetTariff = tariffService.getTariffEntity(request.getTargetTariffId());
            BigDecimal amount = transactionType == BillingTransactionType.TARIFF_SWITCH_FEE
                ? targetTariff.getSwitchFee()
                : targetTariff.getMonthlyFee();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                String currentAmount = task.stringVariable(OPERATION_AMOUNT);
                return variables(OPERATION_AMOUNT, CamundaVariable.string(currentAmount == null ? "0" : currentAmount));
            }

            String description = descriptionPrefix + " (request " + request.getId() + ")";
            if (!billingService.existsBySubscriberIdAndTypeAndDescription(subscriber.getId(), transactionType, description)) {
                subscriber.setBalance(subscriber.getBalance().subtract(amount));
                subscriberService.save(subscriber);
                billingService.createTransaction(subscriber, transactionType, amount, description);
            }

            BigDecimal operationAmount = decimal(task.stringVariable(OPERATION_AMOUNT)).add(amount);
            return variables(OPERATION_AMOUNT, CamundaVariable.string(operationAmount.toPlainString()));
        });
    }

    private Map<String, CamundaVariable> updateSubscriberTariff(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            TariffChangeRequest request = tariffRequest(task);
            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            Tariff targetTariff = tariffService.getTariffEntity(request.getTargetTariffId());
            subscriber.setCurrentTariff(targetTariff);
            subscriberService.save(subscriber);

            request.setStatus(TariffChangeRequestStatus.SUCCESS);
            request.setErrorMessage(null);
            request.setUpdatedAt(OffsetDateTime.now());
            tariffChangeRequestRepository.save(request);
            return variables("operationSuccess", CamundaVariable.bool(true));
        });
    }

    private Map<String, CamundaVariable> validateFeatureDisable(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            FeatureDisableRequest request = featureRequest(task);
            if (isTerminal(request.getStatus())) {
                return variables("canDisableFeature", CamundaVariable.bool(false));
            }

            request.setStatus(TariffChangeRequestStatus.PROCESSING);
            request.setAttemptCount(request.getAttemptCount() + 1);
            request.setErrorMessage(null);
            request.setUpdatedAt(OffsetDateTime.now());
            featureDisableRequestRepository.save(request);

            if (!eisValidationService.allowFeatureDisable(request)) {
                rejectFeatureDisable(request, ApiMessages.FEATURE_DISABLE_REJECTED_BY_EIS);
                return variables("canDisableFeature", CamundaVariable.bool(false));
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
                return variables("canDisableFeature", CamundaVariable.bool(false));
            }

            return variables(
                "canDisableFeature", CamundaVariable.bool(true),
                OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
            );
        });
    }

    private Map<String, CamundaVariable> disableFeatureBilling(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            FeatureDisableRequest request = featureRequest(task);
            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            String description = ApiMessages.FEATURE_DISABLE_BILLING_DESCRIPTION + " (request " + request.getId() + ")";
            if (!billingService.existsBySubscriberIdAndTypeAndDescription(
                subscriber.getId(), BillingTransactionType.SERVICE_DISABLE, description
            )) {
                billingService.createTransaction(subscriber, BillingTransactionType.SERVICE_DISABLE, BigDecimal.ZERO, description);
            }
            return variables(OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString()));
        });
    }

    private Map<String, CamundaVariable> updateSubscriberFeature(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
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
            return variables("operationSuccess", CamundaVariable.bool(true));
        });
    }

    private Map<String, CamundaVariable> createMonthlyFeeRequests() {
        int created = monthlyFeeChargeAsyncService.enqueueCurrentCycleCharges();
        return variables("createdMonthlyFeeRequests", CamundaVariable.integer(created));
    }

    private Map<String, CamundaVariable> createDolibarrInvoice(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            MonthlyFeeChargeRequest request = monthlyRequest(task);
            if (isTerminal(request.getStatus())) {
                return variables("monthlyFeeTerminal", CamundaVariable.bool(true));
            }

            request.setStatus(TariffChangeRequestStatus.PROCESSING);
            request.setAttemptCount(request.getAttemptCount() + 1);
            request.setErrorMessage(null);
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);

            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            Tariff tariff = subscriber.getCurrentTariff();
            if (tariff != null && request.getDolibarrInvoiceId() == null) {
                dolibarrInvoiceService.createUnpaidMonthlyFeeInvoice(
                    subscriber,
                    request.getId(),
                    request.getBillingPeriod(),
                    tariff.getMonthlyFee()
                ).ifPresent(reference -> {
                    request.setDolibarrThirdPartyId(reference.thirdPartyId());
                    request.setDolibarrInvoiceId(reference.invoiceId());
                    request.setDolibarrInvoiceRef(reference.externalRef());
                    request.setUpdatedAt(OffsetDateTime.now());
                    monthlyFeeChargeRequestRepository.save(request);
                });
            }
            return variables(OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString()));
        });
    }

    private Map<String, CamundaVariable> chargeMonthlyFee(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            MonthlyFeeChargeRequest request = monthlyRequest(task);
            if (isTerminal(request.getStatus())) {
                return variables("monthlyFeeSucceeded", CamundaVariable.bool(request.getStatus() == TariffChangeRequestStatus.SUCCESS));
            }

            Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
            Tariff tariff = subscriber.getCurrentTariff();
            if (tariff == null) {
                rejectMonthlyFee(
                    request,
                    subscriber,
                    ApiMessages.MONTHLY_FEE_MISSING_TARIFF_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                    ApiMessages.MONTHLY_FEE_MISSING_TARIFF_NOTIFICATION_PREFIX + request.getBillingPeriod()
                );
                return variables("monthlyFeeSucceeded", CamundaVariable.bool(false));
            }

            BigDecimal monthlyFee = tariff.getMonthlyFee();
            String description = ApiMessages.MONTHLY_FEE_CHARGE_DESCRIPTION_PREFIX + request.getBillingPeriod();
            if (billingService.existsBySubscriberIdAndTypeAndDescription(
                subscriber.getId(), BillingTransactionType.MONTHLY_TARIFF_FEE, description
            )) {
                request.setStatus(TariffChangeRequestStatus.SUCCESS);
                request.setUpdatedAt(OffsetDateTime.now());
                monthlyFeeChargeRequestRepository.save(request);
                return variables(
                    "monthlyFeeSucceeded", CamundaVariable.bool(true),
                    OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
                );
            }

            if (subscriber.getBalance().compareTo(monthlyFee) < 0) {
                rejectMonthlyFee(
                    request,
                    subscriber,
                    ApiMessages.MONTHLY_FEE_INSUFFICIENT_FUNDS_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                    ApiMessages.TARIFF_INSUFFICIENT_FUNDS_RESPONSE
                );
                return variables("monthlyFeeSucceeded", CamundaVariable.bool(false));
            }

            subscriber.setBalance(subscriber.getBalance().subtract(monthlyFee));
            subscriberService.save(subscriber);
            billingService.createTransaction(subscriber, BillingTransactionType.MONTHLY_TARIFF_FEE, monthlyFee, description);
            notificationService.createNotification(
                subscriber,
                NotificationType.MONTHLY_FEE_CHARGED,
                ApiMessages.MONTHLY_FEE_CHARGED_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                true
            );
            request.setStatus(TariffChangeRequestStatus.SUCCESS);
            request.setErrorMessage(null);
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);

            return variables(
                "monthlyFeeSucceeded", CamundaVariable.bool(true),
                OPERATION_AMOUNT, CamundaVariable.string(monthlyFee.toPlainString())
            );
        });
    }

    private Map<String, CamundaVariable> syncDolibarrInvoice(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            MonthlyFeeChargeRequest request = monthlyRequest(task);
            if (request.getStatus() == TariffChangeRequestStatus.SUCCESS && request.getDolibarrInvoiceId() != null) {
                boolean changed = dolibarrInvoiceService.markInvoicePaid(request.getDolibarrInvoiceId());
                if (!changed) {
                    log.warn(
                        "Dolibarr invoice status sync failed: requestId={}, invoiceId={}",
                        request.getId(),
                        request.getDolibarrInvoiceId()
                    );
                }
            }
            return variables();
        });
    }

    private Map<String, CamundaVariable> sendNotification(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            String operationType = task.stringVariable(OPERATION_TYPE);
            if (TARIFF_CHANGE.equals(operationType)) {
                TariffChangeRequest request = tariffRequest(task);
                if (request.getStatus() == TariffChangeRequestStatus.SUCCESS) {
                    notificationService.createNotification(
                        subscriberService.getSubscriberEntity(request.getSubscriberId()),
                        NotificationType.TARIFF_CHANGED,
                        ApiMessages.TARIFF_CHANGED_NOTIFICATION,
                        true
                    );
                }
            } else if (FEATURE_DISABLE.equals(operationType)) {
                FeatureDisableRequest request = featureRequest(task);
                if (request.getStatus() == TariffChangeRequestStatus.SUCCESS) {
                    notificationService.createNotification(
                        subscriberService.getSubscriberEntity(request.getSubscriberId()),
                        NotificationType.SERVICE_DISABLED,
                        ApiMessages.FEATURE_DISABLED_NOTIFICATION,
                        true
                    );
                }
            }
            return variables();
        });
    }

    private Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task) {
        return businessTransactionTemplate.execute(status -> {
            String operationType = task.stringVariable(OPERATION_TYPE);
            BigDecimal amount = decimal(task.stringVariable(OPERATION_AMOUNT));
            if (TARIFF_CHANGE.equals(operationType)) {
                TariffChangeRequest request = tariffRequest(task);
                publishTariffChangeAudit(request, amount);
            } else if (FEATURE_DISABLE.equals(operationType)) {
                FeatureDisableRequest request = featureRequest(task);
                publishFeatureDisableAudit(request, amount);
            } else if (MONTHLY_FEE.equals(operationType)) {
                MonthlyFeeChargeRequest request = monthlyRequest(task);
                publishMonthlyFeeAudit(request, amount);
            }
            return variables();
        });
    }

    private void rejectTariffChange(TariffChangeRequest request, String message) {
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
    }

    private void rejectFeatureDisable(FeatureDisableRequest request, String message) {
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
    }

    private void rejectMonthlyFee(MonthlyFeeChargeRequest request, Subscriber subscriber, String notification, String error) {
        notificationService.createNotification(subscriber, NotificationType.MONTHLY_FEE_ERROR, notification, false);
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(error);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
    }

    private void publishTariffChangeAudit(TariffChangeRequest request, BigDecimal amount) {
        if (isTerminal(request.getStatus())) {
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
    }

    private void publishFeatureDisableAudit(FeatureDisableRequest request, BigDecimal amount) {
        if (isTerminal(request.getStatus())) {
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
    }

    private void publishMonthlyFeeAudit(MonthlyFeeChargeRequest request, BigDecimal amount) {
        if (isTerminal(request.getStatus())) {
            eisOperationAuditService.registerOperationResult(new EisOperationResult(
                EisOperationType.MONTHLY_FEE_CHARGE_REQUESTED,
                request.getId(),
                request.getSubscriberId(),
                amount,
                request.getStatus(),
                request.getErrorMessage(),
                OffsetDateTime.now()
            ));
        }
    }

    private TariffChangeRequest tariffRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return tariffChangeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("TariffChangeRequest not found: " + id));
    }

    private FeatureDisableRequest featureRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return featureDisableRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("FeatureDisableRequest not found: " + id));
    }

    private MonthlyFeeChargeRequest monthlyRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return monthlyFeeChargeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("MonthlyFeeChargeRequest not found: " + id));
    }

    private boolean isTerminal(TariffChangeRequestStatus status) {
        return status == TariffChangeRequestStatus.SUCCESS
            || status == TariffChangeRequestStatus.REJECTED
            || status == TariffChangeRequestStatus.FAILED;
    }

    private void markFailed(TariffChangeRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
    }

    private void markFailed(FeatureDisableRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        featureDisableRequestRepository.save(request);
    }

    private void markFailed(MonthlyFeeChargeRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
    }

    private BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private Map<String, CamundaVariable> variables(Object... keyValues) {
        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            variables.put((String) keyValues[i], (CamundaVariable) keyValues[i + 1]);
        }
        return variables;
    }
}
