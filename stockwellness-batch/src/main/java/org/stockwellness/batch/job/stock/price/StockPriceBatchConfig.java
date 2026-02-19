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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public Job stockPriceBatchJob(StockPriceProcessor stockPriceProcessor) {
        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceStep(stockPriceProcessor))
                .build();
    }

    @Bean
    public Step stockPriceStep(StockPriceProcessor stockPriceProcessor) {
        return new StepBuilder("stockPriceStep", jobRepository)
                // [중요 1] Input: Stock, Output: List<StockPrice> 로 명시
                .<Stock, List<StockPrice>>chunk(1, transactionManager) // [중요 2] 1개 종목(1,250행) 단위로 커밋
                .reader(stockReader())
                .processor(stockPriceProcessor)
                // [중요 3] 커스텀 ListWriter로 감싸서 등록
                .writer(stockPriceListWriter())
                .listener(progressListener)
                .faultTolerant()
                // [수정] 일시적인 외부 API 장애나 DB 데드락 등에 대해서만 재시도
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .retry(RecoverableDataAccessException.class)
                .build();
    }

    // 1. Reader: 활성화된 종목만 읽어옴
    @Bean
    public JpaPagingItemReader<Stock> stockReader() {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM Stock s WHERE s.status = 'ACTIVE'")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemWriter<List<StockPrice>> stockPriceListWriter() {
        return chunk -> {
            // 1. Chunk<List<StockPrice>>를 평탄화(Flatten) -> List<StockPrice>
            List<StockPrice> flatList = new ArrayList<>();
            for (List<StockPrice> list : chunk) {
                if (list != null) {
                    flatList.addAll(list);
                }
            }

            // 2. 실제 DB 저장을 담당하는 JDBC Writer에게 낱개 리스트를 위임
            if (!flatList.isEmpty()) {
                stockPriceJdbcWriter().write(new Chunk<>(flatList));
            }
        };
    }

    private JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter() {
        return new JdbcBatchItemWriterBuilder<StockPrice>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO stock_price (
                        stock_id, base_date, open_price, high_price, low_price, close_price, 
                        adj_close_price, volume, transaction_amt,
                        ma5, ma20, ma60, ma120, rsi14, macd, macd_signal, created_at
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, 
                        ?, ?, ?, 
                        ?, ?, ?, ?, ?, ?, ?, NOW()
                    )
                    ON CONFLICT (stock_id, base_date) 
                    DO UPDATE SET 
                        close_price = EXCLUDED.close_price,
                        adj_close_price = EXCLUDED.adj_close_price,
                        volume = EXCLUDED.volume,
                        transaction_amt = EXCLUDED.transaction_amt,
                        ma5 = EXCLUDED.ma5,
                        ma20 = EXCLUDED.ma20,
                        ma60 = EXCLUDED.ma60,
                        ma120 = EXCLUDED.ma120,
                        rsi14 = EXCLUDED.rsi14,
                        macd = EXCLUDED.macd,
                        macd_signal = EXCLUDED.macd_signal
                    """)
                .itemPreparedStatementSetter((item, ps) -> {
                    // 1. ID & Date (복합키에서 추출)
                    ps.setLong(1, item.getId().getStockId());
                    ps.setDate(2, java.sql.Date.valueOf(item.getId().getBaseDate()));

                    // 2. OHLCV Data
                    ps.setBigDecimal(3, item.getOpenPrice());
                    ps.setBigDecimal(4, item.getHighPrice());
                    ps.setBigDecimal(5, item.getLowPrice());
                    ps.setBigDecimal(6, item.getClosePrice());
                    ps.setBigDecimal(7, item.getAdjClosePrice());
                    ps.setLong(8, item.getVolume());
                    ps.setBigDecimal(9, item.getTransactionAmount());

                    // 3. Technical Indicators (Null Safety 처리)
                    var indicators = item.getIndicators();
                    ps.setBigDecimal(10, indicators != null ? indicators.getMa5() : null);
                    ps.setBigDecimal(11, indicators != null ? indicators.getMa20() : null);
                    ps.setBigDecimal(12, indicators != null ? indicators.getMa60() : null);
                    ps.setBigDecimal(13, indicators != null ? indicators.getMa120() : null);
                    ps.setBigDecimal(14, indicators != null ? indicators.getRsi14() : null);
                    ps.setBigDecimal(15, indicators != null ? indicators.getMacd() : null);
                    ps.setBigDecimal(16, indicators != null ? indicators.getMacdSignal() : null);

                    // (created_at은 쿼리에서 NOW()로 처리하므로 파라미터 세팅 불필요)
                })
                .build();
    }
}
