package ru.urasha.callmeani.blps.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {

    @NotBlank
    private String monthlyFeeCyclePattern = "yyyy-MM-dd-HH:mm";
}
