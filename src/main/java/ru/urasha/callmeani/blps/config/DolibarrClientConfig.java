package ru.urasha.callmeani.blps.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DolibarrProperties.class)
public class DolibarrClientConfig {

    @Bean(name = "dolibarrRestClient")
    public RestClient dolibarrRestClient(RestClient.Builder builder, DolibarrProperties dolibarrProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(dolibarrProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(dolibarrProperties.getReadTimeoutMs());

        return builder
            .requestFactory(requestFactory)
            .baseUrl(normalizeBaseUrl(dolibarrProperties.getUrl()) + "/api/index.php")
            .defaultHeader("DOLAPIKEY", dolibarrProperties.getApiKey())
            .build();
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        if (rawUrl.endsWith("/")) {
            return rawUrl.substring(0, rawUrl.length() - 1);
        }
        return rawUrl;
    }
}

