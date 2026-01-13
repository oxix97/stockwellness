package org.stockwellness.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class LlmConfig {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.timeout.read}")
    private long readTimeout;

    @Bean
    public RestClient aiRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout((int) readTimeout);

        return builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}