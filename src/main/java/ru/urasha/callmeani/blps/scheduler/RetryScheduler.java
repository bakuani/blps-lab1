package ru.urasha.callmeani.blps.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.service.billing.async.MonthlyFeeChargeAsyncOperations;
import ru.urasha.callmeani.blps.service.feature.async.FeatureDisableAsyncOperations;
import ru.urasha.callmeani.blps.service.tariff.async.TariffChangeAsyncOperations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static ru.urasha.callmeani.blps.logging.LoggingFields.CORRELATION_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.scheduler.retry-enabled", havingValue = "true")
@RequiredArgsConstructor
public class RetryScheduler {

    private final TariffChangeAsyncOperations tariffChangeAsyncService;
    private final FeatureDisableAsyncOperations featureDisableAsyncService;
    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;

    @Scheduled(cron = "${app.scheduler.retry-cron:0 */5 * * * *}")
    @Transactional
    public void retryStuckOperations() {
        try (LoggingContext ignored = LoggingContext.open(
            CORRELATION_ID, UUID.randomUUID(),
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "retry_scheduler"
        )) {
            try {
                OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(1);
                List<TariffChangeRequestStatus> targetStatuses = List.of(
                    TariffChangeRequestStatus.PENDING,
                    TariffChangeRequestStatus.RETRY
                );
                int tariffRetried = tariffChangeAsyncService.retryStuckOperations(threshold, targetStatuses);
                int featureRetried = featureDisableAsyncService.retryStuckOperations(threshold, targetStatuses);
                int monthlyRetried = monthlyFeeChargeAsyncService.retryStuckOperations(threshold, targetStatuses);
                int totalRetried = tariffRetried + featureRetried + monthlyRetried;
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "success")) {
                    log.info(
                        "Retry scheduler completed: tariff={}, feature={}, monthly={}, total={}",
                        tariffRetried, featureRetried, monthlyRetried, totalRetried
                    );
                }
            } catch (RuntimeException ex) {
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "failure")) {
                    log.error("Retry scheduler failed", ex);
                }
                throw ex;
            }
        }
    }
}
