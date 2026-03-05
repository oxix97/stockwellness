package org.stockwellness.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String STOCK_PRICE_UPDATED_TOPIC = "stock-price-updated";

    @Bean
    public NewTopic stockPriceUpdatedTopic() {
        return TopicBuilder.name(STOCK_PRICE_UPDATED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
