package ru.urasha.callmeani.blps.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    RabbitMqProperties.class,
    SchedulerProperties.class,
    SecurityUsersProperties.class
})
public class ApplicationPropertiesConfig {
}
