package org.stockwellness.adapter.in.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.in.batch.job.StockHistoryUpdateWriter;
import org.stockwellness.adapter.in.batch.job.StockIndicatorBackfillProcessor;
import org.stockwellness.adapter.out.external.krx.KrxClient;
import org.stockwellness.adapter.out.external.krx.dto.StockPriceDto;
import org.stockwellness.application.service.mapper.StockMapper;
import org.stockwellness.domain.stock.StockHistory;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyStockBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final KrxClient krxClient;
    private final StockMapper stockMapper;
    private final DataSource dataSource;
    private final StockIndicatorBackfillProcessor processor;
    private final StockHistoryUpdateWriter writer;

    @Bean
    public Job dailyStockJob() {
        return new JobBuilder("dailyStockJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fetchAndSaveStep())      // Step 1. 적재
                .start(backfillStep()) // Step 2. 계산
//                .listener() Kafka 이벤트 처리
                .build();
    }

    @Bean
    public Step backfillStep() {
        return new StepBuilder("backfillStep", jobRepository)
                .<String, List<StockHistory>>chunk(10, transactionManager) // 종목 10개씩 묶어서 처리 (약 13,000 row 업데이트)
                .reader(stockIsinReader())
                .processor(processor)
                .writer(writer)
                .taskExecutor(new VirtualThreadTaskExecutor()) // Java 21 가상 스레드 필수
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<String> stockIsinReader() {
        JdbcCursorItemReader<String> cursorReader = new JdbcCursorItemReaderBuilder<String>()
                .name("stockIsinReaderRaw") // 이름 변경
                .dataSource(dataSource)
                .sql("SELECT DISTINCT isin_code FROM stock_history ORDER BY isin_code")
                .rowMapper(new SingleColumnRowMapper<>(String.class))
                .build();

        // Reader를 Thread-Safe하게 감싸서 리턴
        return new SynchronizedItemStreamReaderBuilder<String>()
                .delegate(cursorReader)
                .build();
    }

    @Bean
    public PagingQueryProvider queryProvider(DataSource dataSource) throws Exception {
        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("SELECT DISTINCT isin_code");
        provider.setFromClause("FROM stock_history");
        provider.setSortKey("isin_code");

        return provider.getObject();
    }

    // --- Step 1: 데이터 수집 및 저장 ---
    @Bean
    public Step fetchAndSaveStep() {
        return new StepBuilder("fetchAndSaveStep", jobRepository)
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
                                ma_5, ma_20, rsi_14, created_at, updated_at
                            )
                            VALUES 
                            (
                                :isinCode, :baseDate, :closePrice, :openPrice, :highPrice, :lowPrice, 
                                :priceChange, :fluctuationRate, :volume, :tradingValue, :marketCap, 
                                :ma5, :ma20, :rsi14, NOW(), NOW() -- [수정] 파라미터 이름 통일
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
                                rsi_14 = COALESCE(stock_history.rsi_14, EXCLUDED.rsi_14),
                                updated_at = NOW()
                        """)
                .beanMapped()
                .build();
    }
}