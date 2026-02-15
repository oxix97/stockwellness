package org.stockwellness.batch.job.stock.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMasterSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final KisMasterClient kisMasterClient;
    private final StockMasterParser stockMasterParser;
    private final JdbcTemplate jdbcTemplate;

    private static final String ALIVE_TICKERS_KEY = "aliveTickers";

    @Bean
    public Job stockMasterSyncJob() {
        return new JobBuilder("stockMasterSyncJob", jobRepository)
                .start(kospiMasterStep())
                .next(kosdaqMasterStep())
                .next(delistedCheckStep())
                .build();
    }

    @Bean
    public Step kospiMasterStep() {
        return new StepBuilder("kospiMasterStep", jobRepository)
                .tasklet(syncTasklet(MarketType.KOSPI), transactionManager)
                .build();
    }

    @Bean
    public Step kosdaqMasterStep() {
        return new StepBuilder("kosdaqMasterStep", jobRepository)
                .tasklet(syncTasklet(MarketType.KOSDAQ), transactionManager)
                .build();
    }

    @Bean
    public Step delistedCheckStep() {
        return new StepBuilder("delistedCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>> Start Checking Delisted Stocks...");

                    // 1. JobContext에서 수집된 Alive Tickers 가져오기
                    ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
                    Set<String> aliveTickers = (Set<String>) jobContext.get(ALIVE_TICKERS_KEY);

                    if (aliveTickers == null || aliveTickers.isEmpty()) {
                        log.warn("Alive tickers set is empty! Skip delisting check to prevent wiping out DB.");
                        return RepeatStatus.FINISHED;
                    }

                    // 2. DB에서 현재 ACTIVE 상태인 모든 종목 조회
                    List<String> dbActiveTickers = jdbcTemplate.queryForList(
                            "SELECT ticker FROM stock WHERE status = 'ACTIVE'",
                            String.class
                    );

                    // 3. 차집합 계산 (DB - Alive = Delisted)
                    List<String> delistedTickers = dbActiveTickers.stream()
                            .filter(ticker -> !aliveTickers.contains(ticker))
                            .toList();

                    if (delistedTickers.isEmpty()) {
                        log.info("No delisted stocks found.");
                        return RepeatStatus.FINISHED;
                    }

                    log.info(">>> Found {} delisted stocks: {}", delistedTickers.size(), delistedTickers);

                    // 4. 상태 업데이트 (ACTIVE -> DELISTED, 추적 중단)
                    int updatedCount = updateDelistedStatus(delistedTickers);

                    log.info(">>> Marked {} stocks as DELISTED.", updatedCount);
                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    // 마켓 타입에 따라 동작하는 범용 Tasklet
    private Tasklet syncTasklet(MarketType marketType) {
        return (contribution, chunkContext) -> {
            log.info(">>> Start Downloading & Syncing {} Master File...", marketType);

            // 1. 파일 다운로드 & 라인 읽기
            List<String> lines = (marketType == MarketType.KOSPI)
                    ? kisMasterClient.downloadKospiMaster()
                    : kisMasterClient.downloadKosdaqMaster();

            if (lines.isEmpty()) {
                log.warn(">>> Master file is empty for {}", marketType);
                return RepeatStatus.FINISHED;
            }

            // 2. 파싱 (String -> Stock Entity)
            List<Stock> stocks = lines.stream()
                    .map(line -> stockMasterParser.parseLine(line, marketType))
                    .filter(Objects::nonNull) // 파싱 실패/헤더 제거
                    .toList();

            log.info(">>> Parsed {} stocks. Starting Bulk Upsert...", stocks.size());

            // 3. DB 적재 (Bulk Upsert)
            bulkUpsertStocks(stocks);

            ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();

            Set<String> aliveSet = (Set<String>) jobContext.get(ALIVE_TICKERS_KEY);
            if (aliveSet == null) {
                aliveSet = new HashSet<>();
            }

            // 현재 마켓의 티커들 추가
            Set<String> currentMarketTickers = stocks.stream().map(Stock::getTicker).collect(Collectors.toSet());
            aliveSet.addAll(currentMarketTickers);

            // 다시 Context에 저장
            jobContext.put(ALIVE_TICKERS_KEY, aliveSet);

            log.info(">>> Finish Syncing {}.", marketType);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * JDBC Batch Update를 이용한 고성능 Upsert
     */
    private void bulkUpsertStocks(List<Stock> stocks) {
        String sql = """
        INSERT INTO stock (
            ticker, standard_code, name, market_type, currency, 
            sector_code, sector_name, status, is_premium_tracking, 
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        ON CONFLICT (ticker) 
        DO UPDATE SET 
            standard_code = EXCLUDED.standard_code,
            name = EXCLUDED.name,
            sector_code = EXCLUDED.sector_code,
            sector_name = EXCLUDED.sector_name,
            status = 'ACTIVE',
            updated_at = NOW()
    """;

        jdbcTemplate.batchUpdate(sql, stocks, 1000, (ps, stock) -> {
            ps.setString(1, stock.getTicker());
            ps.setString(2, stock.getStandardCode());
            ps.setString(3, stock.getName());
            ps.setString(4, stock.getMarketType().name());
            ps.setString(5, stock.getCurrency().name());
            ps.setString(6, stock.getSectorCode());
            ps.setString(7, stock.getSectorName());
            ps.setString(8, stock.getStatus().name());
            ps.setBoolean(9, stock.isPremiumTracking());
        });
    }

    private int updateDelistedStatus(List<String> tickers) {
        // IN 절에 들어갈 파라미터 생성 (?,?,?...)
        String inSql = String.join(",", Collections.nCopies(tickers.size(), "?"));

        String sql = String.format("""
            UPDATE stock 
            SET status = '%s', 
                is_premium_tracking = false, 
                updated_at = NOW() 
            WHERE ticker IN (%s)
        """, StockStatus.DELISTED.name(), inSql);

        return jdbcTemplate.update(sql, tickers.toArray());
    }
}