package org.stockwellness.batch.job.insight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.consumer.group-id=batch-test-group")
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"market-score-calculated"})
class MarketWeatherJobIntegrationTest {

    @Autowired
    private Job dailyMarketWeatherJob;

    @Autowired
    private Job backfillMarketWeatherJob;

    @Test
    @DisplayName("시장 기상 배치 Job이 빈으로 등록되어 있어야 한다")
    void jobBeans_shouldBeRegistered() {
        assertThat(dailyMarketWeatherJob).isNotNull();
        assertThat(backfillMarketWeatherJob).isNotNull();
    }
}
