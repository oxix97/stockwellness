package org.stockwellness.application.stockprice.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.application.stockprice.step.processor.DailyStockPriceProcessor;
import org.stockwellness.application.stockprice.step.reader.StockListReader;
import org.stockwellness.application.stockprice.step.writer.StockPriceListWriter;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;


@Configuration
public class DailyStockPriceStepConfig {

    /**
     * [Step 2-1] 시세 수집 단계: [KIS] 멀티종목시세 조회를 통해 원시 가격 데이터를 저장
     */
    @Bean
    public Step dailyStockPriceStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,
            StockListReader dailyStockPriceReader,
            DailyStockPriceProcessor dailyStockPriceProcessor,
            StockPriceListWriter stockPriceListWriter
    ) {
        return new StepBuilder("dailyStockPriceStep", jobRepository)
                .<List<Stock>, List<StockPrice>>chunk(1, txManager)
                .reader(dailyStockPriceReader)
                .processor(dailyStockPriceProcessor)
                .writer(stockPriceListWriter)
                .build();
    }

    @Bean
    @StepScope
    public StockListReader dailyStockPriceReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public DailyStockPriceProcessor dailyStockPriceProcessor(KisDailyPriceAdapter kisDailyPriceAdapter) {
        return new DailyStockPriceProcessor(kisDailyPriceAdapter);
    }

    @Bean
    public StockPriceListWriter dailyStockPriceWriter(JdbcTemplate template, JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter) {
        return new StockPriceListWriter(template, stockPriceJdbcWriter);
    }
}
