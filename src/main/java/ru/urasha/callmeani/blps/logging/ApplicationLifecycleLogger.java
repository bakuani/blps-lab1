package ru.urasha.callmeani.blps.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;

@Slf4j
@Component
public class ApplicationLifecycleLogger {

    private final Environment environment;

    public ApplicationLifecycleLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "configuration",
            EVENT_ACTION, "application_started",
            EVENT_OUTCOME, "success"
        )) {
            log.info(
                "BLPS application started: profiles={}, camundaEnabled={}, monthlyFeeSchedulerEnabled={}, retrySchedulerEnabled={}",
                Arrays.toString(environment.getActiveProfiles()),
                environment.getProperty("app.camunda.enabled"),
                environment.getProperty("app.scheduler.monthly-fee-enabled"),
                environment.getProperty("app.scheduler.retry-enabled")
            );
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onApplicationStopped() {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "configuration",
            EVENT_ACTION, "application_stopped",
            EVENT_OUTCOME, "success"
        )) {
            log.info("BLPS application stopped");
        }
    }
}
