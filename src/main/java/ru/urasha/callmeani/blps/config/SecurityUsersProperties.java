package ru.urasha.callmeani.blps.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.users")
public class SecurityUsersProperties {

    @NotBlank
    private String xmlPath = "classpath:security/users.xml";
}
