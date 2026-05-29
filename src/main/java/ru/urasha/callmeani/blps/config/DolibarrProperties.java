package ru.urasha.callmeani.blps.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "eis.dolibarr")
public class DolibarrProperties {

    @NotBlank
    private String url;

    @NotBlank
    private String apiKey;

    private boolean failClosed = true;

    @Min(1)
    private int connectTimeoutMs = 3000;

    @Min(1)
    private int readTimeoutMs = 5000;
}

