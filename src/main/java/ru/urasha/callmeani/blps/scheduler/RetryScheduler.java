package ru.urasha.callmeani.blps.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.messaging.FeatureDisableRequestedMessage;
import ru.urasha.callmeani.blps.messaging.MonthlyFeeChargeRequestedMessage;
import ru.urasha.callmeani.blps.messaging.TariffChangeRequestedMessage;
import ru.urasha.callmeani.blps.repository.FeatureDisableRequestRepository;
import ru.urasha.callmeani.blps.repository.MonthlyFeeChargeRequestRepository;
import ru.urasha.callmeani.blps.repository.TariffChangeRequestRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryScheduler {

    private final TariffChangeRequestRepository tariffChangeRequestRepository;
    private final FeatureDisableRequestRepository featureDisableRequestRepository;
    private final MonthlyFeeChargeRequestRepository monthlyFeeChargeRequestRepository;
    private final JmsTemplate jmsTemplate;

    @Value("${app.jms.tariff-change-queue}")
    private String tariffChangeQueue;

    @Value("${app.jms.feature-disable-queue}")
    private String featureDisableQueue;

    @Value("${app.jms.monthly-fee-queue}")
    private String monthlyFeeQueue;

    @Scheduled(cron = "${app.scheduler.retry-cron:0 */5 * * * *}")
    @Transactional
    public void retryStuckOperations() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(1);
        List<TariffChangeRequestStatus> targetStatuses = List.of(TariffChangeRequestStatus.PENDING, TariffChangeRequestStatus.RETRY);

        List<TariffChangeRequest> stuckTariffChanges = tariffChangeRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        for (TariffChangeRequest request : stuckTariffChanges) {
            log.info("Retrying TariffChangeRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(tariffChangeQueue, new TariffChangeRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        tariffChangeRequestRepository.saveAll(stuckTariffChanges);

        List<FeatureDisableRequest> stuckFeatureDisables = featureDisableRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        for (FeatureDisableRequest request : stuckFeatureDisables) {
            log.info("Retrying FeatureDisableRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(featureDisableQueue, new FeatureDisableRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        featureDisableRequestRepository.saveAll(stuckFeatureDisables);

        List<MonthlyFeeChargeRequest> stuckMonthlyFees = monthlyFeeChargeRequestRepository.findByStatusInAndUpdatedAtBefore(targetStatuses, threshold);
        for (MonthlyFeeChargeRequest request : stuckMonthlyFees) {
            log.info("Retrying MonthlyFeeChargeRequest with id {}", request.getId());
            jmsTemplate.convertAndSend(monthlyFeeQueue, new MonthlyFeeChargeRequestedMessage(request.getId()));
            request.setUpdatedAt(OffsetDateTime.now());
        }
        monthlyFeeChargeRequestRepository.saveAll(stuckMonthlyFees);
    }
}