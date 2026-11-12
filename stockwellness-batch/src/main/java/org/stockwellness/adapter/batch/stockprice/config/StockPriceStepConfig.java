package org.stockwellness.adapter.batch.stockprice.config;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.batch.stockprice.step.reader.StockListReader;
import org.stockwellness.adapter.batch.stockprice.step.writer.StockPriceListWriter;
import org.stockwellness.adapter.batch.stockprice.support.StockPriceBatchTargetQuery;
import org.stockwellness.application.port.in.batch.NewMarketDataSyncUseCase;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.global.util.DateUtil;

@Configuration
@RequiredArgsConstructor
public class StockPriceStepConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Step dailyStockPriceStep(StockListReader dailyStockPriceReader,
            StockPriceListWriter stockPriceListWriter) {
        return new StepBuilder("dailyStockPriceStep", jobRepository)
                .<List<Stock>, List<Stock>>chunk(1, txManager)
                .reader(dailyStockPriceReader)
                .writer(stockPriceListWriter)
                .build();
    }

    @Bean
    @StepScope
    public StockPriceListWriter stockPriceListWriter(NewMarketDataSyncUseCase sync,
            @Value("#{jobParameters['targetDate']}") String targetDate,
            @Value("#{jobParameters['endDate']}") String endDate) {
        return new StockPriceListWriter(sync, parseTargetDate(targetDate, endDate));
    }

    private LocalDate parseTargetDate(String targetDateStr, String endDateStr) {
        LocalDate targetDate = DateUtil.parse(targetDateStr);
        if (targetDate == null) targetDate = DateUtil.parse(endDateStr);
        return targetDate == null ? DateUtil.today() : targetDate;
    }

    @Bean
    @StepScope
    public StockListReader dailyStockPriceReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> stockReader(@Value("#{jobParameters['targetTicker']}") String targetTicker) {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(StockPriceBatchTargetQuery.selectQuery(targetTicker))
                .parameterValues(StockPriceBatchTargetQuery.parameters(targetTicker))
                .pageSize(300)
                .saveState(false)
                .build();
    }
}
