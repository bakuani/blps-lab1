package ru.urasha.callmeani.blps.service.billing.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.urasha.callmeani.blps.api.dto.billing.MonthlyFeeChargeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.exception.MonthlyFeeChargeRequestNotFoundException;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.messaging.MonthlyFeeChargeRequestedMessage;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.eis.DolibarrInvoiceService;
import ru.urasha.callmeani.blps.service.eis.EisOperationAuditService;
import ru.urasha.callmeani.blps.eis.model.EisOperationResult;
import ru.urasha.callmeani.blps.eis.model.EisOperationType;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyFeeChargeAsyncService implements MonthlyFeeChargeAsyncOperations {

    private final SubscriberService subscriberService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final DolibarrInvoiceService dolibarrInvoiceService;
    private final EisOperationAuditService eisOperationAuditService;
    private final JmsTemplate jmsTemplate;
    @Qualifier("businessTransactionTemplate")
    private final TransactionTemplate businessTransactionTemplate;

    @Value("${app.jms.monthly-fee-queue}")
    private String monthlyFeeQueue;

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
            jmsTemplate.convertAndSend(monthlyFeeQueue, new MonthlyFeeChargeRequestedMessage(saved.getId()));
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

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyFeeChargeRequestStatusResponse> getRecentStatuses(Long subscriberId) {
        return monthlyFeeChargeRequestRepository.findTop10BySubscriberIdOrderByCreatedAtDesc(subscriberId)
            .stream()
            .map(request -> new MonthlyFeeChargeRequestStatusResponse(
                request.getId(),
                request.getBillingPeriod(),
                request.getStatus(),
                request.getErrorMessage(),
                request.getAttemptCount(),
                request.getDolibarrThirdPartyId(),
                request.getDolibarrInvoiceId(),
                request.getDolibarrInvoiceRef(),
                request.getUpdatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional
    public int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses) {
        List<MonthlyFeeChargeRequest> stuckRequests = monthlyFeeChargeRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        for (MonthlyFeeChargeRequest request : stuckRequests) {
            log.info("Retrying MonthlyFeeChargeRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(monthlyFeeQueue, new MonthlyFeeChargeRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        monthlyFeeChargeRequestRepository.saveAll(stuckRequests);
        return stuckRequests.size();
    }

    @JmsListener(destination = "${app.jms.monthly-fee-queue}", concurrency = "2")
    public void processMonthlyFeeCharge(MonthlyFeeChargeRequestedMessage message) {
        MonthlyFeeChargeRequest request = monthlyFeeChargeRequestRepository.findById(message.requestId()).orElse(null);
        AtomicReference<BigDecimal> operationAmount = new AtomicReference<>(BigDecimal.ZERO);
        if (request == null) {
            return;
        }
        if (request.getStatus() == TariffChangeRequestStatus.SUCCESS || request.getStatus() == TariffChangeRequestStatus.REJECTED) {
            return;
        }

        request.setStatus(TariffChangeRequestStatus.PROCESSING);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setErrorMessage(null);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);

        try {
            businessTransactionTemplate.executeWithoutResult(status -> {
                Subscriber subscriber = subscriberService.getSubscriberEntity(request.getSubscriberId());
                Tariff tariff = subscriber.getCurrentTariff();
                if (tariff == null) {
                    rejectRequest(
                        request,
                        ApiMessages.MONTHLY_FEE_MISSING_TARIFF_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                        subscriber
                    );
                    return;
                }

                BigDecimal monthlyFee = tariff.getMonthlyFee();
                String billingDescription = ApiMessages.MONTHLY_FEE_CHARGE_DESCRIPTION_PREFIX + request.getBillingPeriod();

                if (billingService.existsBySubscriberIdAndTypeAndDescription(
                    subscriber.getId(), BillingTransactionType.MONTHLY_TARIFF_FEE, billingDescription
                )) {
                    request.setStatus(TariffChangeRequestStatus.SUCCESS);
                    request.setUpdatedAt(OffsetDateTime.now());
                    monthlyFeeChargeRequestRepository.save(request);
                    return;
                }

                ensureDolibarrInvoiceCreated(request, subscriber, monthlyFee);

                if (subscriber.getBalance().compareTo(monthlyFee) < 0) {
                    notificationService.createNotification(
                        subscriber,
                        NotificationType.MONTHLY_FEE_ERROR,
                        ApiMessages.MONTHLY_FEE_INSUFFICIENT_FUNDS_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                        false
                    );
                    request.setStatus(TariffChangeRequestStatus.REJECTED);
                    request.setErrorMessage(ApiMessages.TARIFF_INSUFFICIENT_FUNDS_RESPONSE);
                    request.setUpdatedAt(OffsetDateTime.now());
                    monthlyFeeChargeRequestRepository.save(request);
                    return;
                }

                subscriber.setBalance(subscriber.getBalance().subtract(monthlyFee));
                subscriberService.save(subscriber);
                billingService.createTransaction(subscriber, BillingTransactionType.MONTHLY_TARIFF_FEE, monthlyFee, billingDescription);
                notificationService.createNotification(
                    subscriber,
                    NotificationType.MONTHLY_FEE_CHARGED,
                    ApiMessages.MONTHLY_FEE_CHARGED_NOTIFICATION_PREFIX + request.getBillingPeriod(),
                    true
                );
                operationAmount.set(monthlyFee);
                request.setStatus(TariffChangeRequestStatus.SUCCESS);
                request.setUpdatedAt(OffsetDateTime.now());
                monthlyFeeChargeRequestRepository.save(request);
            });
        } catch (RuntimeException ex) {
            log.error("Monthly fee charge request {} failed", request.getId(), ex);
            if (ex instanceof ru.urasha.callmeani.blps.api.exception.NotFoundException) {
                request.setStatus(TariffChangeRequestStatus.REJECTED);
            } else {
                request.setStatus(request.getAttemptCount() < 3 ? TariffChangeRequestStatus.RETRY : TariffChangeRequestStatus.FAILED);
            }
            request.setErrorMessage(ex.getMessage());
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);
        } finally {
            syncDolibarrInvoiceStatus(request);
            publishEisResult(request, operationAmount.get());
        }
    }

    private void ensureDolibarrInvoiceCreated(MonthlyFeeChargeRequest request, Subscriber subscriber, BigDecimal monthlyFee) {
        if (request.getDolibarrInvoiceId() != null) {
            return;
        }

        dolibarrInvoiceService.createUnpaidMonthlyFeeInvoice(
            subscriber,
            request.getId(),
            request.getBillingPeriod(),
            monthlyFee
        ).ifPresent(reference -> {
            request.setDolibarrThirdPartyId(reference.thirdPartyId());
            request.setDolibarrInvoiceId(reference.invoiceId());
            request.setDolibarrInvoiceRef(reference.externalRef());
            request.setUpdatedAt(OffsetDateTime.now());
            monthlyFeeChargeRequestRepository.save(request);
        });
    }

    private void rejectRequest(MonthlyFeeChargeRequest request, String message, Subscriber subscriber) {
        notificationService.createNotification(subscriber, NotificationType.MONTHLY_FEE_ERROR, message, false);
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
    }

    private void syncDolibarrInvoiceStatus(MonthlyFeeChargeRequest request) {
        if (!isTerminalStatus(request.getStatus())) {
            return;
        }

        Long invoiceId = request.getDolibarrInvoiceId();
        if (invoiceId == null) {
            return;
        }

        if (request.getStatus() != TariffChangeRequestStatus.SUCCESS) {
            return;
        }

        boolean changed = dolibarrInvoiceService.markInvoicePaid(invoiceId);

        if (!changed) {
            log.warn(
                "Dolibarr invoice status sync failed: requestId={}, invoiceId={}, finalStatus={}",
                request.getId(),
                invoiceId,
                request.getStatus()
            );
        }
    }

    private void publishEisResult(MonthlyFeeChargeRequest request, BigDecimal amount) {
        if (!isTerminalStatus(request.getStatus())) {
            return;
        }
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

    private boolean isTerminalStatus(TariffChangeRequestStatus status) {
        return status == TariffChangeRequestStatus.SUCCESS
            || status == TariffChangeRequestStatus.REJECTED
            || status == TariffChangeRequestStatus.FAILED;
    }
}
