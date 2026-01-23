package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.krx.KrxClient;
import org.stockwellness.adapter.out.external.krx.dto.StockPriceDto;
import org.stockwellness.adapter.in.batch.mapper.StockBatchMapper;
import org.stockwellness.domain.stock.StockHistory;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockHistoryFetch {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final KrxClient krxClient;
    private final StockBatchMapper stockMapper;
    private final DataSource dataSource;

    @Bean
    public Step fetchAndSaveStock() {
        return new StepBuilder("fetchAndSaveStock", jobRepository)
                .<StockPriceDto, StockHistory>chunk(1000, transactionManager)
                .reader(dailyStockPriceReader(null))
                .processor(dailyStockPriceProcessor())
                .writer(dailyStockPriceWriter(dataSource))
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<StockPriceDto> dailyStockPriceReader(
            @Value("#{jobParameters['requestDate']}") String requestDate
    ) {
        String dateStr = (requestDate != null) ? requestDate : LocalDate.now().toString().replace("-", "");
        List<StockPriceDto> items = krxClient.stockPriceResponse(dateStr).getItems();

        if (items == null || items.isEmpty()) {
            log.warn("KRX Data is empty for date: {}", dateStr);
        }

        return new ListItemReader<>(items != null ? items : List.of());
    }

    @Bean
    public ItemProcessor<StockPriceDto, StockHistory> dailyStockPriceProcessor() {
        return stockMapper::toHistoryEntity;
    }

    @Bean
    public JdbcBatchItemWriter<StockHistory> dailyStockPriceWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<StockHistory>()
                .dataSource(dataSource)
                .sql("""
                            INSERT INTO stock_history 
                            (
                                isin_code, base_date, close_price, open_price, high_price, low_price, 
                                price_change, fluctuation_rate, volume, trading_value, market_cap, 
                                ma_5, ma_20, ma_60, ma_120, rsi_14, created_at, updated_at
                            )
                            VALUES 
                            (
                                :isinCode, :baseDate, :closePrice, :openPrice, :highPrice, :lowPrice, 
                                :priceChange, :fluctuationRate, :volume, :tradingValue, :marketCap, 
                                :ma5, :ma20, :ma60, :ma120, :rsi14, NOW(), NOW()
                            )
                            ON CONFLICT (isin_code, base_date) 
                            DO UPDATE SET
                                close_price = EXCLUDED.close_price,
                                open_price = EXCLUDED.open_price,
                                high_price = EXCLUDED.high_price,
                                low_price = EXCLUDED.low_price,
                                volume = EXCLUDED.volume,
                                market_cap = EXCLUDED.market_cap,
                                fluctuation_rate = EXCLUDED.fluctuation_rate,
                                -- 기존 지표 보존 로직 (재계산 전 임시값 덮어쓰기 방지)
                                ma_5 = COALESCE(stock_history.ma_5, EXCLUDED.ma_5),
                                ma_20 = COALESCE(stock_history.ma_20, EXCLUDED.ma_20),
                                ma_60 = COALESCE(stock_history.ma_60, EXCLUDED.ma_60),
                                ma_120 = COALESCE(stock_history.ma_120, EXCLUDED.ma_120),
                                rsi_14 = COALESCE(stock_history.rsi_14, EXCLUDED.rsi_14),
                                updated_at = NOW()
                        """)
                .beanMapped()
                .build();
    }
}
