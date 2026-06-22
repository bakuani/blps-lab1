package ru.urasha.callmeani.blps.service.billing.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.urasha.callmeani.blps.logging.LoggingContext;

import java.util.UUID;

import static ru.urasha.callmeani.blps.logging.LoggingFields.CORRELATION_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduler.monthly-fee-enabled", havingValue = "true")
@RequiredArgsConstructor
public class MonthlyFeeChargeScheduler {

    private final MonthlyFeeChargeAsyncOperations monthlyFeeChargeAsyncService;

    @Scheduled(cron = "${app.scheduler.monthly-fee-cron}")
    public void scheduleMonthlyFeeCharges() {
        try (LoggingContext ignored = LoggingContext.open(
            CORRELATION_ID, UUID.randomUUID(),
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "monthly_fee_scheduler"
        )) {
            log.info("Monthly fee scheduler started");
            try {
                int queued = monthlyFeeChargeAsyncService.enqueueCurrentCycleCharges();
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "success")) {
                    log.info("Monthly fee scheduler completed: queuedRequests={}", queued);
                }
            } catch (RuntimeException ex) {
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "failure")) {
                    log.error("Monthly fee scheduler failed", ex);
                }
                throw ex;
            }
        }
    }
}
