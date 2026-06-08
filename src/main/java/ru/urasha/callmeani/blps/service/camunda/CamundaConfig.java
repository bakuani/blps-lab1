package ru.urasha.callmeani.blps.service.camunda;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CamundaConfig {

    @Bean
    public RestClient camundaRestClient(CamundaProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }
}
