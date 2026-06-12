package ru.urasha.callmeani.blps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.camunda")
public class CamundaProperties {
    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8082/engine-rest";
    private String workerId = "blps-local-worker";
    private long lockDurationMs = 30_000L;
    private long pollIntervalMs = 2_000L;
    private int maxTasks = 10;
    private int defaultRetries = 3;
    private long retryTimeoutMs = 60_000L;
}
