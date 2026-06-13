package ru.urasha.callmeani.blps.service.tariff.camunda.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.tariff.camunda.TariffChangeCamundaTaskService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CAN_CHANGE_TARIFF;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.HAS_SWITCH_FEE;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_AMOUNT;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_SUCCESS;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.REQUEST_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.of;

@Service
@RequiredArgsConstructor
public class TariffChangeCamundaTaskServiceImpl implements TariffChangeCamundaTaskService {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final SubscriberService subscriberService;
    private final TariffService tariffService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final EisValidationService eisValidationService;
    private final EisOperationAuditService eisOperationAuditService;

    @Override
    @Transactional
    public Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        if (isTerminal(request.getStatus())) {
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
        }

        markProcessing(request);
        if (!eisValidationService.allowTariffChange(request)) {
            rejectTariffChange(request, ApiMessages.TARIFF_CHANGE_REJECTED_BY_EIS);
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
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
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
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
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
        }

        return of(
            CAN_CHANGE_TARIFF, CamundaVariable.bool(true),
            HAS_SWITCH_FEE, CamundaVariable.bool(switchFee.compareTo(BigDecimal.ZERO) > 0),
            OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
        );
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> chargeSwitchFee(LockedExternalTask task) {
        return chargeTariffAmount(task, BillingTransactionType.TARIFF_SWITCH_FEE, ApiMessages.TARIFF_SWITCH_FEE_DESCRIPTION);
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> chargeNewMonthlyFee(LockedExternalTask task) {
        return chargeTariffAmount(task, BillingTransactionType.MONTHLY_TARIFF_FEE, ApiMessages.TARIFF_MONTHLY_FEE_DESCRIPTION);
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> updateSubscriberTariff(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
        Tariff targetTariff = tariffService.getTariffEntity(request.getTargetTariffId());
        subscriber.setCurrentTariff(targetTariff);
        subscriberService.save(subscriber);

        request.setStatus(TariffChangeRequestStatus.SUCCESS);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
        return of(OPERATION_SUCCESS, CamundaVariable.bool(true));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> sendNotification(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS) {
            notificationService.createNotification(
                subscriberService.getSubscriberEntity(request.getSubscriberId()),
                NotificationType.TARIFF_CHANGED,
                ApiMessages.TARIFF_CHANGED_NOTIFICATION,
                true
            );
        }
        return of();
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        if (isTerminal(request.getStatus())) {
            BigDecimal amount = decimal(task.stringVariable(OPERATION_AMOUNT));
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
        return of();
    }

    @Override
    @Transactional
    public void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft) {
        Long requestId = task.longVariable(REQUEST_ID);
        if (requestId == null) {
            return;
        }
        tariffChangeRequestRepository.findById(requestId)
            .ifPresent(request -> markFailed(request, failureStatus(exception, retriesLeft), exception));
    }

    private Map<String, CamundaVariable> chargeTariffAmount(
        LockedExternalTask task,
        BillingTransactionType transactionType,
        String descriptionPrefix
    ) {
        TariffChangeRequest request = tariffRequest(task);
        Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
        Tariff targetTariff = tariffService.getTariffEntity(request.getTargetTariffId());
        BigDecimal amount = transactionType == BillingTransactionType.TARIFF_SWITCH_FEE
            ? targetTariff.getSwitchFee()
            : targetTariff.getMonthlyFee();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            String currentAmount = task.stringVariable(OPERATION_AMOUNT);
            return of(OPERATION_AMOUNT, CamundaVariable.string(currentAmount == null ? "0" : currentAmount));
        }

        String description = descriptionPrefix + " (request " + request.getId() + ")";
        if (!billingService.existsBySubscriberIdAndTypeAndDescription(subscriber.getId(), transactionType, description)) {
            subscriber.setBalance(subscriber.getBalance().subtract(amount));
            subscriberService.save(subscriber);
            billingService.createTransaction(subscriber, transactionType, amount, description);
        }

        BigDecimal operationAmount = decimal(task.stringVariable(OPERATION_AMOUNT)).add(amount);
        return of(OPERATION_AMOUNT, CamundaVariable.string(operationAmount.toPlainString()));
    }

    private void markProcessing(TariffChangeRequest request) {
        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
    }

    private void rejectTariffChange(TariffChangeRequest request, String message) {
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
    }

    private void markFailed(TariffChangeRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        tariffChangeRequestRepository.save(request);
    }

    private TariffChangeRequest tariffRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return tariffChangeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("TariffChangeRequest not found: " + id));
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
