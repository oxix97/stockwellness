package org.stockwellness.adapter.batch.insight;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.insight.SectorIndicator;
import org.stockwellness.adapter.out.persistence.insight.repository.SectorIndicatorRepository;
import org.stockwellness.application.port.out.messaging.MarketScoreCalculatedEvent;
import org.stockwellness.application.service.insight.WeatherScoreCalculator;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MarketWeatherBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final SectorIndicatorRepository sectorIndicatorRepository;
    private final WeatherScoreCalculator weatherScoreCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public Job dailyMarketWeatherJob(
            Step calculateSectorIndicatorStep,
            Step publishMarketScoreEventStep
    ) {
        return new JobBuilder("dailyMarketWeatherJob", jobRepository)
                .start(calculateSectorIndicatorStep)
                .next(publishMarketScoreEventStep)
                .build();
    }

    @Bean
    public Step calculateSectorIndicatorStep(
            JpaPagingItemReader<SectorInsight> sectorInsightReader,
            ItemProcessor<SectorInsight, SectorIndicator> sectorIndicatorProcessor,
            ItemWriter<SectorIndicator> sectorIndicatorWriter
    ) {
        return new StepBuilder("calculateSectorIndicatorStep", jobRepository)
                .<SectorInsight, SectorIndicator>chunk(20, transactionManager)
                .reader(sectorInsightReader)
                .processor(sectorIndicatorProcessor)
                .writer(sectorIndicatorWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SectorInsight> sectorInsightReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr
    ) {
        LocalDate targetDate = DateUtil.parseFlexible(targetDateStr);
        if (targetDate == null) targetDate = DateUtil.today();

        return new JpaPagingItemReaderBuilder<SectorInsight>()
                .name("sectorInsightReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM SectorInsight s WHERE s.baseDate = :targetDate")
                .parameterValues(Map.of("targetDate", targetDate))
                .pageSize(20)
                .build();
    }

    @Bean
    public ItemProcessor<SectorInsight, SectorIndicator> sectorIndicatorProcessor() {
        return insight -> {
            BigDecimal disparity = BigDecimal.ZERO;
            if (insight.getIndicators() != null && insight.getIndicators().getSectorIndexCurrentPrice() != null 
                && insight.getTechnicalIndicators() != null && insight.getTechnicalIndicators().getMa20() != null 
                && insight.getTechnicalIndicators().getMa20().compareTo(BigDecimal.ZERO) > 0) {
                
                disparity = insight.getIndicators().getSectorIndexCurrentPrice()
                        .divide(insight.getTechnicalIndicators().getMa20(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            BigDecimal adr = (insight.getIndicators() != null) ? insight.getIndicators().getAdvanceRatio() : BigDecimal.ZERO;
            BigDecimal rsi = (insight.getTechnicalIndicators() != null) ? insight.getTechnicalIndicators().getRsi14() : null;

            return SectorIndicator.builder()
                    .baseDate(insight.getBaseDate())
                    .sectorCode(insight.getSectorCode())
                    .ma20Disparity(disparity)
                    .adr(adr)
                    .rsi14(rsi)
                    .isOverheated(insight.isOverheated())
                    .build();
        };
    }

    @Bean
    public ItemWriter<SectorIndicator> sectorIndicatorWriter() {
        return items -> sectorIndicatorRepository.saveAll(items);
    }

    @Bean
    public Step publishMarketScoreEventStep() {
        return new StepBuilder("publishMarketScoreEventStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate targetDate = DateUtil.today();
                    
                    // TODO: Calculate actual market score from all sector indicators
                    List<MarketScoreCalculatedEvent.SectorScore> sectorScores = new ArrayList<>();
                    
                    MarketScoreCalculatedEvent event = new MarketScoreCalculatedEvent(
                            targetDate,
                            "KOSPI",
                            75, 
                            sectorScores
                    );
                    
                    kafkaTemplate.send("market-score-calculated", event);
                    log.info("🚀 Published MarketScoreCalculatedEvent for {}", targetDate);
                    
                    return null;
                }, transactionManager)
                .build();
    }
}
