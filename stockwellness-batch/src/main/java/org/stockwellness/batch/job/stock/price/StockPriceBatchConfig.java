package org.stockwellness.batch.job.stock.price;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.QueryTypeUtil;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final StockPriceProgressListener progressListener;
    private final JdbcTemplate jdbcTemplate;

    @Qualifier("batchExecutor")
    private final TaskExecutor batchExecutor;

    @Bean
    public Job stockPriceBatchJob(Step stockPriceStep) {
        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceStep)
                .build();
    }

    @Bean
    public Step stockPriceStep(
            ItemReader<List<Stock>> stockListReader,
            ItemProcessor<List<Stock>, List<StockPrice>> stockPriceProcessor,
            ItemWriter<List<StockPrice>> stockPriceListWriter
    ) {
        return new StepBuilder("stockPriceStep", jobRepository)
                .<List<Stock>, List<StockPrice>>chunk(1, transactionManager) 
                .reader(stockListReader)
                .processor(stockPriceProcessor)
                .writer(stockPriceListWriter)
                .taskExecutor(batchExecutor)
                .listener(progressListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .retry(RecoverableDataAccessException.class)
                .build();
    }

    @Bean
    @StepScope
    public StockListReader stockListReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> stockReader() {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM Stock s WHERE s.status = 'ACTIVE' ORDER BY s.id ASC")
                .pageSize(300)
                .saveState(false)
                .build();
    }

    @Bean
    public ItemWriter<List<StockPrice>> stockPriceListWriter(JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter) {
        return chunk -> {
            List<StockPrice> flatList = new ArrayList<>();
            for (List<StockPrice> list : chunk) {
                if (list != null) flatList.addAll(list);
            }
            if (!flatList.isEmpty()) {
                // [변경] 중복 데이터 삭제 후 삽입 (표준 SQL 방식)
                for (StockPrice sp : flatList) {
                    jdbcTemplate.update("DELETE FROM stock_price WHERE base_date = ? AND stock_id = ?",
                            DateUtil.toSqlDate(sp.getId().getBaseDate()), sp.getId().getStockId());
                }
                stockPriceJdbcWriter.write(new Chunk<>(flatList));
            }
        };
    }

    @Bean
    public JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter() {
        String sql = """
            INSERT INTO stock_price (
                base_date,
                stock_id,
                open_price,
                high_price,
                low_price,
                close_price,
                adj_close_price,
                prev_close_price,
                volume,
                transaction_amt,
                ma5,
                ma20,
                ma60,
                ma120,
                rsi14,
                macd,
                macd_signal,
                created_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP
            )
            """;

        return new JdbcBatchItemWriterBuilder<StockPrice>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setDate(1, DateUtil.toSqlDate(item.getId().getBaseDate()));
                    ps.setLong(2, item.getId().getStockId());
                    ps.setBigDecimal(3, item.getOpenPrice());
                    ps.setBigDecimal(4, item.getHighPrice());
                    ps.setBigDecimal(5, item.getLowPrice());
                    ps.setBigDecimal(6, item.getClosePrice());
                    ps.setBigDecimal(7, item.getAdjClosePrice());
                    ps.setBigDecimal(8, item.getPreviousClosePrice());
                    ps.setLong(9, item.getVolume());
                    ps.setBigDecimal(10, item.getTransactionAmt());
                    var indicators = item.getIndicators();
                    ps.setBigDecimal(11, indicators != null ? indicators.getMa5() : null);
                    ps.setBigDecimal(12, indicators != null ? indicators.getMa20() : null);
                    ps.setBigDecimal(13, indicators != null ? indicators.getMa60() : null);
                    ps.setBigDecimal(14, indicators != null ? indicators.getMa120() : null);
                    ps.setBigDecimal(15, indicators != null ? indicators.getRsi14() : null);
                    ps.setBigDecimal(16, indicators != null ? indicators.getMacd() : null);
                    ps.setBigDecimal(17, indicators != null ? indicators.getMacdSignal() : null);
                })
                .assertUpdates(false)
                .build();
    }
}
