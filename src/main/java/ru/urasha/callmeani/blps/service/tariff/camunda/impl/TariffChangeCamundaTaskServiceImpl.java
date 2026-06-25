package ru.urasha.callmeani.blps.service.tariff.camunda.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.tariff.camunda.TariffChangeCamundaTaskService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPTIONS;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CAN_CHANGE_TARIFF;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CORRELATION_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_TYPE;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.HAS_SWITCH_FEE;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_AMOUNT;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_SUCCESS;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.REQUEST_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.TARGET_TARIFF_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.of;

@Slf4j
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, CamundaVariable> createTariffChangeRequest(LockedExternalTask task) {
        Long existingRequestId = task.longVariable(REQUEST_ID);
        if (existingRequestId != null) {
            TariffChangeRequest request = tariffRequest(task);
            attachProcessInstance(request, task);
            log.info("Tariff change request linked to Camunda process: requestId={}", request.getId());
            return processVariables(request);
        }

        TariffChangeRequest request = new TariffChangeRequest();
        request.setSubscriberId(requiredLongVariable(task, SUBSCRIBER_ID));
        request.setTargetTariffId(requiredLongVariable(task, TARGET_TARIFF_ID));
        request.setOptions(parseOptions(task.stringVariable(OPTIONS)));
        request.setStatus(BusinessRequestStatus.PENDING);
        request.setAttemptCount(0);
        request.setProcessInstanceId(task.processInstanceId());

        TariffChangeRequest saved = tariffChangeRequestRepository.save(request);
        log.info("Tariff change request created from Camunda: requestId={}", saved.getId());
        return processVariables(saved);
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        if (isTerminal(request.getStatus())) {
            log.warn(
                "Tariff change validation skipped for terminal request: requestId={}, status={}",
                request.getId(),
                request.getStatus()
            );
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
        }

        markProcessing(request);
        if (!eisValidationService.allowTariffChange(request)) {
            rejectTariffChange(request, ApiMessages.TARIFF_CHANGE_REJECTED_BY_EIS);
            log.warn("Tariff change rejected: requestId={}, reason=eis_validation", request.getId());
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
            log.warn("Tariff change rejected: requestId={}, reason=tariff_already_selected", request.getId());
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
            log.warn("Tariff change rejected: requestId={}, reason=insufficient_funds", request.getId());
            return of(CAN_CHANGE_TARIFF, CamundaVariable.bool(false));
        }

        log.info(
            "Tariff change validated: requestId={}, switchFee={}, monthlyFee={}",
            request.getId(),
            switchFee,
            monthlyFee
        );
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

        request.setStatus(BusinessRequestStatus.SUCCESS);
        request.setErrorMessage(null);
        tariffChangeRequestRepository.save(request);
        log.info(
            "Subscriber tariff updated: requestId={}, subscriberId={}, tariffId={}",
            request.getId(),
            request.getSubscriberId(),
            request.getTargetTariffId()
        );
        return of(OPERATION_SUCCESS, CamundaVariable.bool(true));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> sendNotification(LockedExternalTask task) {
        TariffChangeRequest request = tariffRequest(task);
        if (request.getStatus() == BusinessRequestStatus.SUCCESS) {
            notificationService.createNotification(
                subscriberService.getSubscriberEntity(request.getSubscriberId()),
                NotificationType.TARIFF_CHANGED,
                ApiMessages.TARIFF_CHANGED_NOTIFICATION,
                true
            );
            log.info("Tariff change notification created: requestId={}", request.getId());
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
            log.info(
                "Tariff change audit published: requestId={}, status={}, amount={}",
                request.getId(),
                request.getStatus(),
                amount
            );
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
            log.info(
                "Tariff charge skipped because amount is zero: requestId={}, transactionType={}",
                request.getId(),
                transactionType
            );
            return of(OPERATION_AMOUNT, CamundaVariable.string(currentAmount == null ? "0" : currentAmount));
        }

        String description = descriptionPrefix + " (request " + request.getId() + ")";
        if (!billingService.existsBySubscriberIdAndTypeAndDescription(subscriber.getId(), transactionType, description)) {
            subscriber.setBalance(subscriber.getBalance().subtract(amount));
            subscriberService.save(subscriber);
            billingService.createTransaction(subscriber, transactionType, amount, description);
            log.info(
                "Tariff charge created: requestId={}, transactionType={}, amount={}",
                request.getId(),
                transactionType,
                amount
            );
        } else {
            log.info(
                "Tariff charge already exists: requestId={}, transactionType={}",
                request.getId(),
                transactionType
            );
        }

        BigDecimal operationAmount = decimal(task.stringVariable(OPERATION_AMOUNT)).add(amount);
        return of(OPERATION_AMOUNT, CamundaVariable.string(operationAmount.toPlainString()));
    }

    private void markProcessing(TariffChangeRequest request) {
        request.setStatus(BusinessRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        tariffChangeRequestRepository.save(request);
    }

    private void rejectTariffChange(TariffChangeRequest request, String message) {
        request.setStatus(BusinessRequestStatus.REJECTED);
        request.setErrorMessage(message);
        tariffChangeRequestRepository.save(request);
    }

    private void markFailed(TariffChangeRequest request, BusinessRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        tariffChangeRequestRepository.save(request);
        if (status == BusinessRequestStatus.FAILED) {
            log.error("Tariff change request permanently failed: requestId={}", request.getId(), exception);
        } else {
            log.warn(
                "Tariff change request failure persisted: requestId={}, status={}, exceptionType={}",
                request.getId(),
                status,
                exception.getClass().getSimpleName()
            );
        }
    }

    private Map<String, CamundaVariable> processVariables(TariffChangeRequest request) {
        return of(
            CORRELATION_ID, CamundaVariable.string(LoggingContext.getOrCreateCorrelationId()),
            REQUEST_ID, CamundaVariable.longValue(request.getId()),
            OPERATION_TYPE, CamundaVariable.string(CamundaProcessConstants.OPERATION_TARIFF_CHANGE),
            SUBSCRIBER_ID, CamundaVariable.longValue(request.getSubscriberId()),
            TARGET_TARIFF_ID, CamundaVariable.longValue(request.getTargetTariffId()),
            OPTIONS, CamundaVariable.string(optionsJson(request.getOptions())),
            OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString())
        );
    }

    private Long requiredLongVariable(LockedExternalTask task, String name) {
        Long value = task.longVariable(name);
        if (value == null) {
            throw new IllegalArgumentException("Camunda variable '" + name + "' is required");
        }
        return value;
    }

    private Map<String, String> parseOptions(String value) {
        if (value == null || value.isBlank() || "{}".equals(value.trim())) {
            return Map.of();
        }
        try {
            Map<String, String> options = objectMapper.readValue(value, new TypeReference<Map<String, String>>() {
            });
            return options == null ? Map.of() : new LinkedHashMap<>(options);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid tariff options JSON. Use object like {\"roaming\":\"enabled\"}", ex);
        }
    }

    private String optionsJson(Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize tariff options", ex);
        }
    }

    private void attachProcessInstance(TariffChangeRequest request, LockedExternalTask task) {
        String processInstanceId = task.processInstanceId();
        if (processInstanceId != null && !processInstanceId.equals(request.getProcessInstanceId())) {
            request.setProcessInstanceId(processInstanceId);
            tariffChangeRequestRepository.save(request);
        }
    }

    private TariffChangeRequest tariffRequest(LockedExternalTask task) {
        Long id = task.longVariable(REQUEST_ID);
        return tariffChangeRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("TariffChangeRequest not found: " + id));
    }

    private boolean isTerminal(BusinessRequestStatus status) {
        return status == BusinessRequestStatus.SUCCESS
            || status == BusinessRequestStatus.REJECTED
            || status == BusinessRequestStatus.FAILED;
    }

    private BusinessRequestStatus failureStatus(RuntimeException exception, int retriesLeft) {
        return exception instanceof NotFoundException
            ? BusinessRequestStatus.REJECTED
            : retriesLeft <= 0 ? BusinessRequestStatus.FAILED : BusinessRequestStatus.RETRY;
    }

    private BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
