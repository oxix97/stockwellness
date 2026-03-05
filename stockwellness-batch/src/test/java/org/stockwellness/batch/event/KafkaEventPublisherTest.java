package org.stockwellness.batch.event;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.stock.event.StockPriceUpdatedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC})
class KafkaEventPublisherTest {

    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testPublishStockPriceUpdated() throws Exception {
        // given
        List<String> symbols = List.of("AAPL", "TSLA");
        
        // Consumer 설정 (발행된 메시지를 확인하기 위함)
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "org.stockwellness.domain.*");
        
        DefaultKafkaConsumerFactory<String, StockPriceUpdatedEvent> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, 
                new StringDeserializer(), 
                new JsonDeserializer<>(StockPriceUpdatedEvent.class)
        );
        
        ContainerProperties containerProperties = new ContainerProperties(KafkaTopicConfig.STOCK_PRICE_UPDATED_TOPIC);
        KafkaMessageListenerContainer<String, StockPriceUpdatedEvent> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
        
        BlockingQueue<ConsumerRecord<String, StockPriceUpdatedEvent>> records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, StockPriceUpdatedEvent>) records::add);
        
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // when
        kafkaEventPublisher.publishStockPriceUpdated(symbols);

        // then
        ConsumerRecord<String, StockPriceUpdatedEvent> received = records.poll(10, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.value().symbols()).containsExactlyInAnyOrderElementsOf(symbols);
        
        container.stop();
    }
}
