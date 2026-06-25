package ru.urasha.callmeani.blps.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "";

    @NotNull
    private Duration expiration = Duration.ofMinutes(15);

    @NotBlank
    private String issuer = "blps";
}
