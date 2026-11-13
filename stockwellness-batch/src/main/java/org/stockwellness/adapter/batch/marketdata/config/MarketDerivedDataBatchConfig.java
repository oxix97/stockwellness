package org.stockwellness.adapter.batch.marketdata.config;

import java.time.LocalDate;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.insight.IndexSeries;

@Configuration
@RequiredArgsConstructor
public class MarketDerivedDataBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job marketDerivedDataJob(@Qualifier("stockTechnicalIndicatorStep") Step stockTechnicalIndicatorStep,
            @Qualifier("indexTechnicalIndicatorStep") Step indexTechnicalIndicatorStep) {
        return new JobBuilder("marketDerivedDataJob", jobRepository)
                .start(stockTechnicalIndicatorStep).next(indexTechnicalIndicatorStep).build();
    }

    @Bean
    public Step stockTechnicalIndicatorStep(@Qualifier("derivedStockReader") JpaPagingItemReader<Stock> derivedStockReader,
            @Qualifier("derivedStockProcessor") ItemProcessor<Stock, Stock> derivedStockProcessor) {
        return new StepBuilder("stockTechnicalIndicatorStep", jobRepository)
                .<Stock, Stock>chunk(1, transactionManager)
                .reader(derivedStockReader).processor(derivedStockProcessor).writer(chunk -> {}).build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> derivedStockReader() {
        return new JpaPagingItemReaderBuilder<Stock>().name("derivedStockReader")
                .entityManagerFactory(entityManagerFactory).pageSize(100)
                .queryString("select s from Stock s where s.marketCode in ('KOSPI','KOSDAQ') and s.status <> :delisted order by s.id")
                .parameterValues(Map.of("delisted", StockStatus.DELISTED)).build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Stock, Stock> derivedStockProcessor(MarketDerivedDataUseCase derived,
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        LocalDate date = LocalDate.parse(targetDate);
        return stock -> {
            derived.calculateStockIndicators(stock.getId(), "KRX", date);
            return stock;
        };
    }

    @Bean
    public Step indexTechnicalIndicatorStep(@Qualifier("derivedIndexSeriesReader") JpaPagingItemReader<IndexSeries> derivedIndexSeriesReader,
            @Qualifier("derivedIndexSeriesProcessor") ItemProcessor<IndexSeries, IndexSeries> derivedIndexSeriesProcessor) {
        return new StepBuilder("indexTechnicalIndicatorStep", jobRepository)
                .<IndexSeries, IndexSeries>chunk(1, transactionManager)
                .reader(derivedIndexSeriesReader).processor(derivedIndexSeriesProcessor).writer(chunk -> {}).build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<IndexSeries> derivedIndexSeriesReader() {
        return new JpaPagingItemReaderBuilder<IndexSeries>().name("derivedIndexSeriesReader")
                .entityManagerFactory(entityManagerFactory).pageSize(100)
                .queryString("select s from IndexSeries s where s.provider = 'KIS' and s.active = true and s.canonical = true "
                        + "and s.returnBasis = 'PRICE' and s.dividendBasis = 'NONE' order by s.id")
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<IndexSeries, IndexSeries> derivedIndexSeriesProcessor(MarketDerivedDataUseCase derived,
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        LocalDate date = LocalDate.parse(targetDate);
        return series -> {
            derived.calculateIndexIndicators(series.getId(), date);
            derived.calculateIndexWeather(series.getId(), date);
            derived.calculateSectorLeadingStocks(series.getId(), date);
            return series;
        };
    }
}
