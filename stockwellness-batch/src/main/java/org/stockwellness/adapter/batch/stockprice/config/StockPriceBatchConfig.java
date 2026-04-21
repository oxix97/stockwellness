package org.stockwellness.adapter.batch.stockprice.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.adapter.batch.stockprice.listener.StockPriceSyncEventListener;
import org.stockwellness.adapter.batch.stockprice.step.processor.DailyStockPriceProcessor;
import org.stockwellness.adapter.batch.stockprice.step.processor.StockInvestorTradeProcessor;
import org.stockwellness.adapter.batch.stockprice.step.processor.TechnicalIndicatorProcessor;
import org.stockwellness.adapter.batch.stockprice.step.reader.StockListReader;
import org.stockwellness.adapter.batch.stockprice.step.writer.StockInvestorTradeListWriter;
import org.stockwellness.adapter.batch.stockprice.step.writer.StockPriceListWriter;
import org.stockwellness.adapter.batch.stockprice.step.writer.StockPriceWriter;
import org.stockwellness.adapter.batch.stockprice.support.StockPriceBatchTargetQuery;
import org.stockwellness.adapter.batch.stockprice.support.StockPriceSql;
import org.stockwellness.batch.support.listener.BatchFailureItemListener;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final JobExecutionListener commonJobListener;
    private final StockPriceSyncEventListener eventListener;

    /**
     * 가격 정보 반영
     * @param dailyStockPriceStep: 멀티종목 시세조회를 통해 가격 정보를 저장한다.
     * @param technicalIndicatorCalculateStep: 기술 지표 계산.
     */
    @Bean
    public Job dailyStockPriceBatchJob(
            Step dailyStockPriceStep,
            Step stockInvestorTradeStep,
            Step technicalIndicatorCalculateStep
    ) {
        return new JobBuilder("dailyStockPriceBatchJob", jobRepository)
                .start(dailyStockPriceStep)
                .next(stockInvestorTradeStep)
                .next(technicalIndicatorCalculateStep)
                .listener(commonJobListener)
                .listener(eventListener)
                .build();
    }

    /**
     * [Step 2-1] 시세 수집 단계: [KIS] 멀티종목시세 조회를 통해 원시 가격 데이터를 저장
     */
    @Bean
    public Step dailyStockPriceStep(
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

    /**
     * [Step 2-2] 수급 수집 단계: [KIS] 멀티종목시세 조회를 통해 수급 데이터를 저장
     */
    @Bean
    public Step stockInvestorTradeStep(
            StockListReader stockInvestorTradeReader,
            StockInvestorTradeProcessor stockInvestorTradeProcessor,
            ItemWriter<List<StockInvestorTrade>> stockInvestorTradeListWriter
    ) {
        return new StepBuilder("stockInvestorTradeStep", jobRepository)
                .<List<Stock>, List<StockInvestorTrade>>chunk(1, txManager)
                .reader(stockInvestorTradeReader)
                .processor(stockInvestorTradeProcessor)
                .writer(stockInvestorTradeListWriter)
                .build();
    }

    /**
     * [Step 2-3] 기술 지표 계산
     */
    @Bean
    public Step technicalIndicatorCalculateStep(
            StockListReader technicalIndicatorListReader,
            TechnicalIndicatorProcessor technicalIndicatorProcessor,
            StockPriceListWriter technicalIndicatorListWriter,
            TaskExecutor batchExecutor
    ) {
        return new StepBuilder("technicalIndicatorCalculateStep", jobRepository)
                .<List<Stock>, List<StockPrice>>chunk(1, txManager)
                .reader(technicalIndicatorListReader)
                .processor(technicalIndicatorProcessor)
                .writer(technicalIndicatorListWriter)
                .taskExecutor(batchExecutor)
                .build();
    }

    @Bean
    @StepScope
    public StockListReader technicalIndicatorListReader(JpaPagingItemReader<Stock> technicalIndicatorReader) {
        return new StockListReader(technicalIndicatorReader, 30);
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
    @StepScope
    public JpaPagingItemReader<Stock> technicalIndicatorReader() {
        JpaPagingItemReader<Stock> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(300);
        reader.setQueryString("""
                select s
                from Stock s
                where s.status = org.stockwellness.domain.stock.StockStatus.ACTIVE
                order by s.id asc
                """);
        reader.setSaveState(false);
        return reader;
    }

    @Bean
    @StepScope
    public StockListReader dailyStockPriceReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public StockListReader stockInvestorTradeReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public StockListReader stockPriceReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public StockListReader stockIndicatorReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public DailyStockPriceProcessor dailyStockPriceProcessor(KisDailyPriceAdapter kisDailyPriceAdapter) {
        return new DailyStockPriceProcessor(kisDailyPriceAdapter);
    }

    @Bean
    @StepScope
    public StockInvestorTradeProcessor stockInvestorTradeProcessor(KisDailyPriceAdapter kisDailyPriceAdapter) {
        return new StockInvestorTradeProcessor(kisDailyPriceAdapter);
    }

    @Bean
    @StepScope
    public TechnicalIndicatorProcessor technicalIndicatorProcessor(StockPricePort stockPricePort) {
        return new TechnicalIndicatorProcessor(stockPricePort);
    }

    @Bean
    public StockPriceListWriter stockPriceListWriter(JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter) {
        return new StockPriceListWriter(jdbcTemplate, stockPriceJdbcWriter);
    }

    @Bean
    public StockPriceWriter technicalIndicatorWriter(StockPricePort stockPricePort) {
        return new StockPriceWriter(stockPricePort);
    }

    @Bean
    public ItemWriter<List<StockInvestorTrade>> stockInvestorTradeListWriter() {
        return new StockInvestorTradeListWriter(jdbcTemplate);
    }

    @Bean
    public BatchFailureItemListener<List<StockInvestorTrade>> stockInvestorTradeFailureListener() {
        return new BatchFailureItemListener<>(result ->
                result.stream()
                        .map(item -> item.getId().getStockId().toString())
                        .toList(),
                result -> result.stream()
                        .map(StockInvestorTrade::getTicker)
                        .reduce((first, second) -> second)
                        .orElse(null)
        );
    }

    @Bean
    public BatchFailureItemListener<List<StockPrice>> stockPriceFailureListener() {
        return new BatchFailureItemListener<>(result ->
                result.stream()
                        .map(item -> item.getId().getStockId().toString())
                        .toList(),
                result -> result.stream()
                        .map(StockPrice::getStock)
                        .filter(Objects::nonNull)
                        .map(Stock::getTicker)
                        .reduce((first, second) -> second)
                        .orElse(null)
        );
    }

    @Bean
    public JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter() {
        return new JdbcBatchItemWriterBuilder<StockPrice>()
                .dataSource(dataSource)
                .sql(StockPriceSql.UPSERT_STOCK_PRICE)
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
                            ps.setNull(idx++, Types.NULL);
                        }
                    }
                })
                .assertUpdates(false)
                .build();
    }
}
