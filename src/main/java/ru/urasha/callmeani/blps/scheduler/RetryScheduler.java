package ru.urasha.callmeani.blps.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;
import ru.urasha.callmeani.blps.service.billing.async.MonthlyFeeChargeAsyncOperations;
import ru.urasha.callmeani.blps.service.feature.async.FeatureDisableAsyncOperations;
import ru.urasha.callmeani.blps.service.tariff.async.TariffChangeAsyncOperations;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryScheduler {

    private final TariffChangeAsyncOperations tariffChangeAsyncService;
    private final FeatureDisableAsyncOperations featureDisableAsyncService;
    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;

    @Scheduled(cron = "${app.scheduler.retry-cron:0 */5 * * * *}")
    @Transactional
    public void retryStuckOperations() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(1);
        List<TariffChangeRequestStatus> targetStatuses = List.of(TariffChangeRequestStatus.PENDING, TariffChangeRequestStatus.RETRY);
        int tariffRetried = tariffChangeAsyncService.retryStuckOperations(threshold, targetStatuses);
        int featureRetried = featureDisableAsyncService.retryStuckOperations(threshold, targetStatuses);
        int monthlyRetried = monthlyFeeChargeAsyncService.retryStuckOperations(threshold, targetStatuses);
        int totalRetried = tariffRetried + featureRetried + monthlyRetried;
        if (totalRetried > 0) {
            log.info(
                "Retry scheduler dispatched operations: tariff={}, feature={}, monthly={}, total={}",
                tariffRetried, featureRetried, monthlyRetried, totalRetried
            );
        }
    }
}
