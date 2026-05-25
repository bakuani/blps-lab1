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
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyFeeChargeAsyncService {

    private final SubscriberRepository subscriberRepository;
    private final SubscriberService subscriberService;
    private final BillingService billingService;
    private final BillingTransactionRepository billingTransactionRepository;
    private final NotificationService notificationService;
    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final JmsTemplate jmsTemplate;
    @Qualifier("businessTransactionTemplate")
    private final TransactionTemplate businessTransactionTemplate;

    @Value("${app.jms.monthly-fee-queue}")
    private String monthlyFeeQueue;

    @Value("${app.scheduler.monthly-fee-cycle-pattern}")
    private String cyclePattern;

    @Transactional
    public int enqueueCurrentCycleCharges() {
        String billingPeriod = DateTimeFormatter.ofPattern(cyclePattern).format(OffsetDateTime.now());
        List<Subscriber> subscribers = subscriberRepository.findByCurrentTariffIsNotNull();
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
            request.getUpdatedAt()
        );
    }

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
                request.getUpdatedAt()
            ))
            .toList();
    }

    @JmsListener(destination = "${app.jms.monthly-fee-queue}", concurrency = "2")
    public void processMonthlyFeeCharge(MonthlyFeeChargeRequestedMessage message) {
        MonthlyFeeChargeRequest request = monthlyFeeChargeRequestRepository.findById(message.requestId()).orElse(null);
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

                if (billingTransactionRepository.existsBySubscriberIdAndTypeAndDescription(
                    subscriber.getId(), BillingTransactionType.MONTHLY_TARIFF_FEE, billingDescription
                )) {
                    request.setStatus(TariffChangeRequestStatus.SUCCESS);
                    request.setUpdatedAt(OffsetDateTime.now());
                    monthlyFeeChargeRequestRepository.save(request);
                    return;
                }

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
        }
    }

    private void rejectRequest(MonthlyFeeChargeRequest request, String message, Subscriber subscriber) {
        notificationService.createNotification(subscriber, NotificationType.MONTHLY_FEE_ERROR, message, false);
        request.setStatus(TariffChangeRequestStatus.REJECTED);
        request.setErrorMessage(message);
        request.setUpdatedAt(OffsetDateTime.now());
        monthlyFeeChargeRequestRepository.save(request);
    }
}
