package org.stockwellness.batch.job.stock.price;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final StockPriceProgressListener progressListener;
    private final StockPriceSyncEventListener eventListener;
    private final JdbcTemplate jdbcTemplate;
    private final TaskExecutor kisBatchExecutor;

    @Bean
    public RateLimiter kisRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(19) // 10 -> 19로 상향 (최대 성능 모드)
                .timeoutDuration(Duration.ofSeconds(20))
                .build();
        return RateLimiterRegistry.of(config).rateLimiter("kisRateLimiter");
    }

    @Bean
    public Job stockPriceBatchJob(Step stockPriceStep) {
        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceStep)
                .listener(eventListener)
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
                .taskExecutor(kisBatchExecutor) // 공용 배치 실행기 사용
                .listener(progressListener)
                .listener(eventListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .retry(RecoverableDataAccessException.class)
                .retry(org.springframework.web.client.RestClientException.class) // API 에러 리트라이 추가
                .build();
    }

    @Bean
    @StepScope
    public StockListReader stockListReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 5);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> stockReader(
            @Value("#{jobParameters['targetTicker']}") String targetTicker
    ) {
        // [개선] 6자리 숫자 티커만 필터링하기 위해 BETWEEN '000000' AND '999999' 사용
        // 알파벳이 포함된 특수 종목(0002C0 등)을 효율적으로 제외하고 가독성 확보
        String query = "SELECT s FROM Stock s " +
                "WHERE s.status = 'ACTIVE' " +
                "AND s.marketType IN ('KOSPI', 'KOSDAQ') " +
                "AND s.groupCode IN ('ST', 'EF', 'EN') " +
                "AND s.ticker BETWEEN '000000' AND '999999'";

        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(targetTicker)) {
            query += " AND s.ticker = :targetTicker";
            parameters.put("targetTicker", targetTicker);
        }

        query += " ORDER BY s.id ASC";

        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(parameters)
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
                jdbcTemplate.batchUpdate(
                        "DELETE FROM stock_price WHERE base_date = CAST(? AS date) AND stock_id = CAST(? AS bigint)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                StockPrice sp = flatList.get(i);
                                ps.setDate(1, DateUtil.toSqlDate(sp.getId().getBaseDate()));
                                ps.setLong(2, sp.getId().getStockId());
                            }

                            @Override
                            public int getBatchSize() {
                                return flatList.size();
                            }
                        }
                );
                stockPriceJdbcWriter.write(new Chunk<>(flatList));
            }
        };
    }

    @Bean
    public JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter() {
        String sql = """
                INSERT INTO stock_price (
                    base_date, stock_id, open_price, high_price, low_price, close_price, adj_close_price, prev_close_price, volume, transaction_amt,
                    ma5, ma20, ma60, ma120, rsi14, macd, macd_signal,
                    bollinger_upper, bollinger_mid, bollinger_lower, adx, plus_di, minus_di,
                    alignment_status, is_golden_cross, is_dead_cross, is_macd_cross,
                    created_at
                ) VALUES (
                    CAST(? AS date), CAST(? AS bigint), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS bigint), CAST(? AS numeric), 
                    CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), 
                    CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), 
                    CAST(? AS varchar), CAST(? AS boolean), CAST(? AS boolean), CAST(? AS boolean), 
                    CURRENT_TIMESTAMP
                )
                """;

        return new JdbcBatchItemWriterBuilder<StockPrice>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    int idx = 1;
                    ps.setDate(idx++, DateUtil.toSqlDate(item.getId().getBaseDate()));
                    ps.setLong(idx++, item.getId().getStockId());
                    ps.setBigDecimal(idx++, item.getOpenPrice());
                    ps.setBigDecimal(idx++, item.getHighPrice());
                    ps.setBigDecimal(idx++, item.getLowPrice());
                    ps.setBigDecimal(idx++, item.getClosePrice());
                    ps.setBigDecimal(idx++, item.getAdjClosePrice());
                    ps.setBigDecimal(idx++, item.getPreviousClosePrice());
                    ps.setLong(idx++, item.getVolume());
                    ps.setBigDecimal(idx++, item.getTransactionAmt());

                    var indicators = item.getIndicators();
                    if (indicators != null) {
                        ps.setBigDecimal(idx++, indicators.getMa5());
                        ps.setBigDecimal(idx++, indicators.getMa20());
                        ps.setBigDecimal(idx++, indicators.getMa60());
                        ps.setBigDecimal(idx++, indicators.getMa120());
                        ps.setBigDecimal(idx++, indicators.getRsi14());
                        ps.setBigDecimal(idx++, indicators.getMacd());
                        ps.setBigDecimal(idx++, indicators.getMacdSignal());
                        ps.setBigDecimal(idx++, indicators.getBollingerUpper());
                        ps.setBigDecimal(idx++, indicators.getBollingerMid());
                        ps.setBigDecimal(idx++, indicators.getBollingerLower());
                        ps.setBigDecimal(idx++, indicators.getAdx());
                        ps.setBigDecimal(idx++, indicators.getPlusDi());
                        ps.setBigDecimal(idx++, indicators.getMinusDi());
                        ps.setString(idx++, indicators.getAlignmentStatus() != null ? indicators.getAlignmentStatus().name() : null);
                        ps.setObject(idx++, indicators.getIsGoldenCross());
                        ps.setObject(idx++, indicators.getIsDeadCross());
                        ps.setObject(idx++, indicators.getIsMacdCross());
                    } else {
                        for (int i = 0; i < 17; i++) {
                            ps.setNull(idx++, java.sql.Types.NULL);
                        }
                    }
                })
                .assertUpdates(false)
                .build();
    }
}
