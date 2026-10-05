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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.in.batch.NewMarketDataSyncUseCase;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.MarketIndexKind;

@Configuration
@RequiredArgsConstructor
public class NewMarketDataBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job kisTradingCalendarSyncJob(Step kisTradingCalendarSyncStep) {
        return new JobBuilder("kisTradingCalendarSyncJob", jobRepository).start(kisTradingCalendarSyncStep).build();
    }

    @Bean
    public Step kisTradingCalendarSyncStep(org.springframework.batch.core.step.tasklet.Tasklet kisTradingCalendarTasklet) {
        return new StepBuilder("kisTradingCalendarSyncStep", jobRepository)
                .tasklet(kisTradingCalendarTasklet, transactionManager).build();
    }

    @Bean
    @StepScope
    public org.springframework.batch.core.step.tasklet.Tasklet kisTradingCalendarTasklet(
            NewMarketDataSyncUseCase sync, @Value("#{jobParameters['baseDate']}") String baseDate) {
        LocalDate date = LocalDate.parse(baseDate);
        return (contribution, context) -> {
            sync.syncTradingCalendar(date);
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job kisEodStockPriceSyncJob(Step kisEodStockPriceStep) {
        return new JobBuilder("kisEodStockPriceSyncJob", jobRepository).start(kisEodStockPriceStep).build();
    }

    @Bean
    public Step kisEodStockPriceStep(JpaPagingItemReader<Stock> kisEodStockReader,
            org.springframework.batch.item.ItemProcessor<Stock, Stock> kisEodStockProcessor) {
        return new StepBuilder("kisEodStockPriceStep", jobRepository)
                .<Stock, Stock>chunk(1, transactionManager)
                .reader(kisEodStockReader).processor(kisEodStockProcessor).writer(chunk -> {}).build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> kisEodStockReader() {
        return new JpaPagingItemReaderBuilder<Stock>().name("kisEodStockReader")
                .entityManagerFactory(entityManagerFactory).pageSize(100)
                .queryString("select s from Stock s where s.marketCode in ('KOSPI','KOSDAQ') and s.status <> :delisted order by s.id")
                .parameterValues(Map.of("delisted", StockStatus.DELISTED)).build();
    }

    @Bean
    @StepScope
    public org.springframework.batch.item.ItemProcessor<Stock, Stock> kisEodStockProcessor(
            NewMarketDataSyncUseCase sync, @Value("#{jobParameters['startDate']}") String start,
            @Value("#{jobParameters['endDate']}") String end) {
        LocalDate from = LocalDate.parse(start);
        LocalDate to = LocalDate.parse(end);
        return stock -> { sync.syncStockPrices(stock, from, to); return stock; };
    }

    @Bean
    public Job kisEodIndexPriceSyncJob(Step kisEodIndexPriceStep) {
        return new JobBuilder("kisEodIndexPriceSyncJob", jobRepository).start(kisEodIndexPriceStep).build();
    }

    @Bean
    public Step kisEodIndexPriceStep(JpaPagingItemReader<MarketIndex> kisEodIndexReader,
            org.springframework.batch.item.ItemProcessor<MarketIndex, MarketIndex> kisEodIndexProcessor) {
        return new StepBuilder("kisEodIndexPriceStep", jobRepository)
                .<MarketIndex, MarketIndex>chunk(1, transactionManager)
                .reader(kisEodIndexReader).processor(kisEodIndexProcessor).writer(chunk -> {}).build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<MarketIndex> kisEodIndexReader() {
        return new JpaPagingItemReaderBuilder<MarketIndex>().name("kisEodIndexReader")
                .entityManagerFactory(entityManagerFactory).pageSize(20)
                .queryString("select i from MarketIndex i where i.provider = 'KIS' and i.marketCode in ('KOSPI','KOSDAQ') and i.indexKind = :kind order by i.id")
                .parameterValues(Map.of("kind", MarketIndexKind.MARKET)).build();
    }

    @Bean
    @StepScope
    public org.springframework.batch.item.ItemProcessor<MarketIndex, MarketIndex> kisEodIndexProcessor(
            NewMarketDataSyncUseCase sync, @Value("#{jobParameters['startDate']}") String start,
            @Value("#{jobParameters['endDate']}") String end) {
        LocalDate from = LocalDate.parse(start);
        LocalDate to = LocalDate.parse(end);
        return index -> { sync.syncIndexPrices(index, from, to); return index; };
    }
}
