package org.stockwellness.batch.job.stock.price;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

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

    @Qualifier("batchExecutor")
    private final TaskExecutor batchExecutor;

    @Bean
    public Job stockPriceBatchJob(StockPriceProcessor stockPriceProcessor) {
        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceStep(stockPriceProcessor))
                .build();
    }

    @Bean
    public Step stockPriceStep(StockPriceProcessor stockPriceProcessor) {
        return new StepBuilder("stockPriceStep", jobRepository)
                .<List<Stock>, List<StockPrice>>chunk(1, transactionManager) 
                .reader(stockListReader())
                .processor(stockPriceProcessor)
                .writer(stockPriceListWriter(stockPriceJdbcWriter()))
                .taskExecutor(batchExecutor) // 전역 공통 풀 사용
                .listener(progressListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .retry(RecoverableDataAccessException.class)
                .build();
    }

    // [최적화] 30개 종목씩 묶어서 반환하는 리더
    @Bean
    public StockListReader stockListReader() {
        return new StockListReader(stockReader(), 30);
    }

    @Bean
    public JpaPagingItemReader<Stock> stockReader() {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM Stock s WHERE s.status = 'ACTIVE' ORDER BY s.id ASC")
                .pageSize(300) // Chunk Size(30)보다 넉넉하게 설정
                .saveState(false) // 상태 저장 안함
                .build();
    }

    @Bean
    public ItemWriter<List<StockPrice>> stockPriceListWriter(JdbcBatchItemWriter<StockPrice> jdbcWriter) {
        return chunk -> {
            List<StockPrice> flatList = new ArrayList<>();
            for (List<StockPrice> list : chunk) {
                if (list != null) flatList.addAll(list);
            }
            if (!flatList.isEmpty()) {
                jdbcWriter.write(new Chunk<>(flatList));
            }
        };
    }

    @Bean
    public JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter() {
        return new JdbcBatchItemWriterBuilder<StockPrice>()
                .dataSource(dataSource)
                .sql("""
                    MERGE INTO stock_price AS t
                    USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)) 
                        AS s (stock_id, base_date, open_price, high_price, low_price, close_price, 
                              adj_close_price, prev_close_price, volume, transaction_amt, 
                              ma5, ma20, ma60, ma120, rsi14, macd, macd_signal)
                    ON t.stock_id = s.stock_id AND t.base_date = s.base_date
                    WHEN MATCHED THEN
                        UPDATE SET 
                            close_price = s.close_price,
                            adj_close_price = s.adj_close_price,
                            prev_close_price = s.prev_close_price,
                            volume = s.volume,
                            transaction_amt = s.transaction_amt,
                            ma5 = s.ma5,
                            ma20 = s.ma20,
                            ma60 = s.ma60,
                            ma120 = s.ma120,
                            rsi14 = s.rsi14,
                            macd = s.macd,
                            macd_signal = s.macd_signal
                    WHEN NOT MATCHED THEN
                        INSERT (stock_id, base_date, open_price, high_price, low_price, close_price, 
                                adj_close_price, prev_close_price, volume, transaction_amt, 
                                ma5, ma20, ma60, ma120, rsi14, macd, macd_signal, created_at)
                        VALUES (s.stock_id, s.base_date, s.open_price, s.high_price, s.low_price, s.close_price, 
                                s.adj_close_price, s.prev_close_price, s.volume, s.transaction_amt, 
                                s.ma5, s.ma20, s.ma60, s.ma120, s.rsi14, s.macd, s.macd_signal, CURRENT_TIMESTAMP)
                    """)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.getId().getStockId());
                    ps.setDate(2, java.sql.Date.valueOf(item.getId().getBaseDate()));
                    ps.setBigDecimal(3, item.getOpenPrice());
                    ps.setBigDecimal(4, item.getHighPrice());
                    ps.setBigDecimal(5, item.getLowPrice());
                    ps.setBigDecimal(6, item.getClosePrice());
                    ps.setBigDecimal(7, item.getAdjClosePrice());
                    ps.setBigDecimal(8, item.getPreviousClosePrice());
                    ps.setLong(9, item.getVolume());
                    ps.setBigDecimal(10, item.getTransactionAmount());
                    var indicators = item.getIndicators();
                    ps.setBigDecimal(11, indicators != null ? indicators.getMa5() : null);
                    ps.setBigDecimal(12, indicators != null ? indicators.getMa20() : null);
                    ps.setBigDecimal(13, indicators != null ? indicators.getMa60() : null);
                    ps.setBigDecimal(14, indicators != null ? indicators.getMa120() : null);
                    ps.setBigDecimal(15, indicators != null ? indicators.getRsi14() : null);
                    ps.setBigDecimal(16, indicators != null ? indicators.getMacd() : null);
                    ps.setBigDecimal(17, indicators != null ? indicators.getMacdSignal() : null);
                })
                .assertUpdates(false) // ON CONFLICT DO UPDATE behavior varies between DBs
                .build();
    }
}
