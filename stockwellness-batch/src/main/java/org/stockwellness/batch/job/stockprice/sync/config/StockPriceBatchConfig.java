package org.stockwellness.batch.job.stockprice.sync.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.batch.job.stockprice.sync.listener.StockPriceSyncEventListener;
import org.stockwellness.batch.job.stockprice.sync.step.processor.StockPriceProcessor;
import org.stockwellness.batch.job.stockprice.sync.step.reader.StockListReader;
import org.stockwellness.batch.job.stockprice.sync.step.writer.StockPriceListWriter;
import org.stockwellness.batch.job.stockprice.sync.support.StockPriceBatchTargetQuery;
import org.stockwellness.batch.support.BatchMdcListener;
import org.stockwellness.batch.support.lifecycle.BatchLifecycleKafkaListener;
import org.stockwellness.batch.support.listener.BatchFailureItemListener;
import org.stockwellness.batch.support.listener.BatchResultCaptureListener;
import org.stockwellness.batch.support.listener.JobFailureNotificationListener;
import org.stockwellness.batch.support.logging.CommonBatchJobLoggingListener;
import org.stockwellness.batch.support.logging.CommonBatchProgressLoggingListener;
import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final StockPriceSyncEventListener eventListener;
    private final BatchMdcListener mdcListener;
    private final CommonBatchJobLoggingListener commonBatchJobLoggingListener;
    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;
    private final CommonBatchProgressLoggingListener commonBatchProgressLoggingListener;
    private final BatchLifecycleKafkaListener batchLifecycleKafkaListener;
    private final BatchResultCaptureListener resultCaptureListener;
    private final JobFailureNotificationListener failureNotificationListener;
    private final JdbcTemplate jdbcTemplate;
    private final TaskExecutor kisBatchExecutor;

    @Bean
    public Job stockPriceBatchJob(
            Step stockPriceStep
    ) {
        Flow stockPriceFlow = new FlowBuilder<Flow>("stockPriceFlow")
                .start(stockPriceStep)
                .build();

        return new JobBuilder("stockPriceBatchJob", jobRepository)
                .start(stockPriceFlow)
                .end()
                .listener(mdcListener)
                .listener(commonBatchJobLoggingListener)
                .listener(batchLifecycleKafkaListener)
                .listener(failureNotificationListener)
                .listener(eventListener)
                .listener(resultCaptureListener)
                .build();
                }

                @Bean
                public Step stockPriceStep(
            StockListReader stockListReader,
            StockPriceProcessor stockPriceProcessor,
            ItemWriter<List<StockPrice>> stockPriceListWriter,
            BatchFailureItemListener<List<StockPrice>> stockPriceFailureListener
    ) {
        return new StepBuilder("stockPriceStep", jobRepository)
                .<List<Stock>, List<StockPrice>>chunk(1, transactionManager)
                .reader(stockListReader)
                .processor(stockPriceProcessor)
                .writer(stockPriceListWriter)
                .taskExecutor(kisBatchExecutor)
                .listener(mdcListener)
                .listener(commonBatchStepLoggingListener)
                .listener(commonBatchProgressLoggingListener)
                .listener(eventListener)
                .listener(stockPriceFailureListener)
                .faultTolerant()
                .retryLimit(3)
                .retry(TransientDataAccessException.class)
                .retry(RecoverableDataAccessException.class)
                .retry(KisApiException.class)
                .build();
    }

    @Bean
    public BatchFailureItemListener<List<StockPrice>> stockPriceFailureListener() {
        return new BatchFailureItemListener<>(list ->
                list.stream()
                        .map(item -> item.getId().getStockId().toString())
                        .toList(),
                list -> list.stream()
                        .map(StockPrice::getStock)
                        .filter(Objects::nonNull)
                        .map(Stock::getTicker)
                        .reduce((first, second) -> second)
                        .orElse(null)
        );
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
        String query = StockPriceBatchTargetQuery.selectQuery(targetTicker);
        Map<String, Object> parameters = StockPriceBatchTargetQuery.parameters(targetTicker);

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
        return new StockPriceListWriter(jdbcTemplate, stockPriceJdbcWriter);
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
