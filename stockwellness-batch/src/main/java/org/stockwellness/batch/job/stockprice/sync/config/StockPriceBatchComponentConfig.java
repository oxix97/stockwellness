package org.stockwellness.batch.job.stockprice.sync.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.batch.job.stockprice.sync.step.processor.StockPriceProcessor;
import org.stockwellness.batch.job.stockprice.sync.step.reader.StockListReader;
import org.stockwellness.batch.job.stockprice.sync.step.writer.StockPriceListWriter;
import org.stockwellness.batch.job.stockprice.sync.support.StockPriceBatchTargetQuery;
import org.stockwellness.batch.job.stockprice.sync.support.StockPriceSql;
import org.stockwellness.batch.support.listener.BatchFailureItemListener;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class StockPriceBatchComponentConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

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
    public StockListReader stockListReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public StockListReader stockIndicatorReader(JpaPagingItemReader<Stock> stockReader) {
        return new StockListReader(stockReader, 30);
    }

    @Bean
    @StepScope
    public StockPriceProcessor stockPriceFetchProcessor(StockPriceSyncUseCase stockPriceSyncUseCase) {
        return new StockPriceProcessor(stockPriceSyncUseCase, StockPriceProcessor.Mode.FETCH);
    }

    @Bean
    @StepScope
    public StockPriceProcessor stockPriceIndicatorProcessor(StockPriceSyncUseCase stockPriceSyncUseCase) {
        return new StockPriceProcessor(stockPriceSyncUseCase, StockPriceProcessor.Mode.INDICATOR);
    }

    @Bean
    public ItemWriter<List<StockPrice>> stockPriceListWriter(JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter) {
        return new StockPriceListWriter(jdbcTemplate, stockPriceJdbcWriter);
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
