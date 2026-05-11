package org.stockwellness.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

@EnableAsync
@Configuration
public class SlackConfig {

    @Bean(name = "alertExecutor")
    public Executor alertExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "slackRestClient")
    public RestClient slackRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
