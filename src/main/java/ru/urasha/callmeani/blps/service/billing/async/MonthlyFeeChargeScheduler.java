package ru.urasha.callmeani.blps.service.billing.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyFeeChargeScheduler {

    private final MonthlyFeeChargeAsyncService monthlyFeeChargeAsyncService;

    @Scheduled(cron = "${app.scheduler.monthly-fee-cron}")
    public void scheduleMonthlyFeeCharges() {
        int queued = monthlyFeeChargeAsyncService.enqueueCurrentCycleCharges();
        if (queued > 0) {
            log.info("Queued {} monthly fee charge request(s)", queued);
        }
    }
}
