package ru.urasha.callmeani.blps.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.camunda")
public class CamundaProperties {
    private boolean enabled = true;
    @NotBlank
    private String baseUrl = "http://127.0.0.1:8082/engine-rest";
    @NotBlank
    private String workerId = "blps-local-worker";
    @Min(1)
    private long lockDurationMs = 30_000L;
    @Min(1)
    private long pollIntervalMs = 2_000L;
    @Min(1)
    private int maxTasks = 10;
    @Min(0)
    private int defaultRetries = 3;
    @Min(1)
    private long retryTimeoutMs = 60_000L;
}
