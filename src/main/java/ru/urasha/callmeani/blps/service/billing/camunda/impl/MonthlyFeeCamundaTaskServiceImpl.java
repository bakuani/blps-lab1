package ru.urasha.callmeani.blps.service.billing.camunda.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.billing.async.MonthlyFeeChargeAsyncOperations;
import ru.urasha.callmeani.blps.service.billing.camunda.MonthlyFeeCamundaTaskService;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.eis.DolibarrInvoiceService;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.BILLING_PERIOD;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CREATED_MONTHLY_FEE_REQUESTS;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.CORRELATION_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.MONTHLY_FEE_SUCCEEDED;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.MONTHLY_FEE_TERMINAL;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_AMOUNT;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.OPERATION_TYPE;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.REQUEST_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyFeeCamundaTaskServiceImpl implements MonthlyFeeCamundaTaskService {

    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final SubscriberService subscriberService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final DolibarrInvoiceService dolibarrInvoiceService;
    private final EisOperationAuditService eisOperationAuditService;
    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;

    @Value("${app.scheduler.monthly-fee-cycle-pattern}")
    private String cyclePattern;

    @Override
    @Transactional
    public Map<String, CamundaVariable> createMonthlyFeeRequests() {
        int created = monthlyFeeChargeAsyncService.enqueueCurrentCycleCharges();
        log.info("Monthly fee cycle created charge requests: count={}", created);
        return of(CREATED_MONTHLY_FEE_REQUESTS, CamundaVariable.integer(created));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> ensureMonthlyFeeRequest(LockedExternalTask task) {
        Long existingRequestId = task.longVariable(REQUEST_ID);
        if (existingRequestId != null) {
            MonthlyFeeChargeRequest request = monthlyRequest(task);
            attachProcessInstance(request, task);
            log.info("Monthly fee request linked to Camunda process: requestId={}", request.getId());
            return processVariables(request);
        }

        OffsetDateTime now = OffsetDateTime.now();
        MonthlyFeeChargeRequest request = new MonthlyFeeChargeRequest();
        request.setSubscriberId(requiredLongVariable(task, SUBSCRIBER_ID));
        request.setBillingPeriod(resolveBillingPeriod(task));
        request.setStatus(TariffChangeRequestStatus.PENDING);
        request.setAttemptCount(0);
        request.setProcessInstanceId(task.processInstanceId());
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        MonthlyFeeChargeRequest saved = monthlyFeeChargeRequestRepository.save(request);
        log.info("Monthly fee request created from Camunda: requestId={}", saved.getId());
        return processVariables(saved);
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> createDolibarrInvoice(LockedExternalTask task) {
        MonthlyFeeChargeRequest request = monthlyRequest(task);
        if (isTerminal(request.getStatus())) {
            log.warn(
                "Dolibarr invoice creation skipped for terminal monthly fee request: requestId={}, status={}",
                request.getId(),
                request.getStatus()
            );
            return of(MONTHLY_FEE_TERMINAL, CamundaVariable.bool(true));
        }

        markProcessing(request);
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
                log.info(
                    "Dolibarr invoice created: requestId={}, thirdPartyId={}, invoiceId={}",
                    request.getId(),
                    reference.thirdPartyId(),
                    reference.invoiceId()
                );
            });
        }
        return of(OPERATION_AMOUNT, CamundaVariable.string(BigDecimal.ZERO.toPlainString()));
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> chargeMonthlyFee(LockedExternalTask task) {
        MonthlyFeeChargeRequest request = monthlyRequest(task);
        if (isTerminal(request.getStatus())) {
            log.warn(
                "Monthly fee charge skipped for terminal request: requestId={}, status={}",
                request.getId(),
                request.getStatus()
            );
            return of(MONTHLY_FEE_SUCCEEDED, CamundaVariable.bool(request.getStatus() == TariffChangeRequestStatus.SUCCESS));
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
            return of(MONTHLY_FEE_SUCCEEDED, CamundaVariable.bool(false));
        }

        BigDecimal monthlyFee = tariff.getMonthlyFee();
        String description = ApiMessages.MONTHLY_FEE_CHARGE_DESCRIPTION_PREFIX + request.getBillingPeriod();
        if (billingService.existsBySubscriberIdAndTypeAndDescription(
            subscriber.getId(), BillingTransactionType.MONTHLY_TARIFF_FEE, description
        )) {
            request.setStatus(TariffChangeRequestStatus.SUCCESS);
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);
            log.info("Monthly fee charge already exists: requestId={}", request.getId());
            return of(
                MONTHLY_FEE_SUCCEEDED, CamundaVariable.bool(true),
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
            return of(MONTHLY_FEE_SUCCEEDED, CamundaVariable.bool(false));
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
        log.info(
            "Monthly fee charged: requestId={}, subscriberId={}, amount={}, billingPeriod={}",
            request.getId(),
            request.getSubscriberId(),
            monthlyFee,
            request.getBillingPeriod()
        );

        return of(
            MONTHLY_FEE_SUCCEEDED, CamundaVariable.bool(true),
            OPERATION_AMOUNT, CamundaVariable.string(monthlyFee.toPlainString())
        );
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> syncDolibarrInvoice(LockedExternalTask task) {
        MonthlyFeeChargeRequest request = monthlyRequest(task);
        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS && request.getDolibarrInvoiceId() != null) {
            boolean changed = dolibarrInvoiceService.markInvoicePaid(request.getDolibarrInvoiceId());
            if (!changed) {
                log.warn(
                    "Dolibarr invoice status sync failed: requestId={}, invoiceId={}",
                    request.getId(),
                    request.getDolibarrInvoiceId()
                );
            } else {
                log.info(
                    "Dolibarr invoice marked as paid: requestId={}, invoiceId={}",
                    request.getId(),
                    request.getDolibarrInvoiceId()
                );
            }
        }
        return of();
    }

    @Override
    @Transactional
    public Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task) {
        MonthlyFeeChargeRequest request = monthlyRequest(task);
        if (isTerminal(request.getStatus())) {
            BigDecimal amount = decimal(task.stringVariable(OPERATION_AMOUNT));
            eisOperationAuditService.registerOperationResult(new EisOperationResult(
                EisOperationType.MONTHLY_FEE_CHARGE_REQUESTED,
                request.getId(),
                request.getSubscriberId(),
                amount,
                request.getStatus(),
                request.getErrorMessage(),
                OffsetDateTime.now()
            ));
            log.info(
                "Monthly fee audit published: requestId={}, status={}, amount={}",
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
        monthlyFeeChargeRequestRepository.findById(requestId)
            .ifPresent(request -> markFailed(request, failureStatus(exception, retriesLeft), exception));
    }

    private void markProcessing(MonthlyFeeChargeRequest request) {
        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
    }

    private void rejectMonthlyFee(MonthlyFeeChargeRequest request, Subscriber subscriber, String notification, String error) {
        notificationService.createNotification(subscriber, NotificationType.MONTHLY_FEE_ERROR, notification, false);
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(error);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
        log.warn(
            "Monthly fee rejected: requestId={}, subscriberId={}, reason={}",
            request.getId(),
            request.getSubscriberId(),
            error
        );
    }

    private void markFailed(MonthlyFeeChargeRequest request, TariffChangeRequestStatus status, RuntimeException exception) {
        request.setStatus(status);
        request.setErrorMessage(exception.getMessage());
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
        if (status == TariffChangeRequestStatus.FAILED) {
            log.error("Monthly fee request permanently failed: requestId={}", request.getId(), exception);
        } else {
            log.warn(
                "Monthly fee request failure persisted: requestId={}, status={}, exceptionType={}",
                request.getId(),
                status,
                exception.getClass().getSimpleName()
            );
        }
    }

    private Map<String, CamundaVariable> processVariables(MonthlyFeeChargeRequest request) {
        return of(
            CORRELATION_ID, CamundaVariable.string(LoggingContext.getOrCreateCorrelationId()),
            REQUEST_ID, CamundaVariable.longValue(request.getId()),
            OPERATION_TYPE, CamundaVariable.string(CamundaProcessConstants.OPERATION_MONTHLY_FEE),
            SUBSCRIBER_ID, CamundaVariable.longValue(request.getSubscriberId()),
            BILLING_PERIOD, CamundaVariable.string(request.getBillingPeriod()),
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

    private String resolveBillingPeriod(LockedExternalTask task) {
        String billingPeriod = task.stringVariable(BILLING_PERIOD);
        if (billingPeriod == null || billingPeriod.isBlank()) {
            return DateTimeFormatter.ofPattern(cyclePattern).format(OffsetDateTime.now());
        }
        return billingPeriod;
    }

    private void attachProcessInstance(MonthlyFeeChargeRequest request, LockedExternalTask task) {
        String processInstanceId = task.processInstanceId();
        if (processInstanceId != null && !processInstanceId.equals(request.getProcessInstanceId())) {
            request.setProcessInstanceId(processInstanceId);
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);
        }
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

    private TariffChangeRequestStatus failureStatus(RuntimeException exception, int retriesLeft) {
        return exception instanceof NotFoundException
            ? TariffChangeRequestStatus.REJECTED
            : retriesLeft <= 0 ? TariffChangeRequestStatus.FAILED : TariffChangeRequestStatus.RETRY;
    }

    private BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
