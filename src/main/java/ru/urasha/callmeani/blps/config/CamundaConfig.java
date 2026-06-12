package ru.urasha.callmeani.blps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CamundaConfig {

    @Bean(name = "camundaEngineRestClient")
    public RestClient camundaEngineRestClient(CamundaProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }
}
