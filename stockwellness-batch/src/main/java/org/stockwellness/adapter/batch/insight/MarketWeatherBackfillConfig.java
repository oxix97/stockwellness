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
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.insight.SectorIndicator;
import org.stockwellness.adapter.out.persistence.insight.repository.SectorIndicatorRepository;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MarketWeatherBackfillConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final SectorIndicatorRepository sectorIndicatorRepository;

    @Bean
    public Job backfillMarketWeatherJob(Step backfillSectorIndicatorStep) {
        return new JobBuilder("backfillMarketWeatherJob", jobRepository)
                .start(backfillSectorIndicatorStep)
                .build();
    }

    @Bean
    public Step backfillSectorIndicatorStep(
            JpaPagingItemReader<SectorInsight> backfillReader,
            ItemProcessor<SectorInsight, SectorIndicator> backfillProcessor,
            ItemWriter<SectorIndicator> backfillWriter
    ) {
        return new StepBuilder("backfillSectorIndicatorStep", jobRepository)
                .<SectorInsight, SectorIndicator>chunk(50, transactionManager)
                .reader(backfillReader)
                .processor(backfillProcessor)
                .writer(backfillWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SectorInsight> backfillReader(
            @Value("#{jobParameters['startDate']}") String startDateStr
    ) {
        LocalDate startDate = DateUtil.parseFlexible(startDateStr);
        if (startDate == null) startDate = DateUtil.today().minusDays(365);

        log.info("🚀 Starting Market Weather Backfill from {}", startDate);

        return new JpaPagingItemReaderBuilder<SectorInsight>()
                .name("backfillReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM SectorInsight s WHERE s.baseDate >= :startDate ORDER BY s.baseDate ASC")
                .parameterValues(Map.of("startDate", startDate))
                .pageSize(50)
                .build();
    }

    @Bean
    public ItemProcessor<SectorInsight, SectorIndicator> backfillProcessor() {
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
    public ItemWriter<SectorIndicator> backfillWriter() {
        return items -> sectorIndicatorRepository.saveAll(items);
    }
}
