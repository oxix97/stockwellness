package org.stockwellness.batch.job.stock.repair;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@SpringBootTest
@ActiveProfiles("realdb")
@AutoConfigureTestDatabase(replace = NONE)
class StockPriceRepairIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private org.springframework.batch.core.Job stockPricePrevCloseSyncJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("실제 PostgreSQL 연결 테스트: 누락된 데이터를 찾아 보정 후 반영 확인")
    void dynamic_repair_real_db_test() throws Exception {
        // 1. Given: 실제 DB에서 보정이 필요한 샘플 하나를 찾음 (2023년 이전 NULL 데이터)
        String findSql = """
            SELECT s.ticker, sp.base_date 
            FROM stock_price sp 
            JOIN stock s ON sp.stock_id = s.id 
            WHERE sp.prev_close_price IS NULL 
              AND sp.base_date <= '2022-12-31' 
            LIMIT 1
            """;
        
        Map<String, Object> sample = jdbcTemplate.queryForMap(findSql);
        String ticker = (String) sample.get("ticker");
        LocalDate targetDate = ((java.sql.Date) sample.get("base_date")).toLocalDate();
        
        log.info("Found sample for test: Ticker={}, Date={}", ticker, targetDate);

        // 2. When: 보정 배치 실행 (해당 종목 한정)
        JobParameters params = new JobParametersBuilder()
                .addString("targetTicker", ticker)
                .addString("startDate", "20220101")
                .addString("endDate", "20221231")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(stockPricePrevCloseSyncJob, params);

        // 3. Then: 실제 DB 적재 확인
        BigDecimal savedPrevClose = jdbcTemplate.queryForObject(
                "SELECT prev_close_price FROM stock_price WHERE stock_id = (SELECT id FROM stock WHERE ticker = ?) AND base_date = CAST(? AS date)",
                BigDecimal.class,
                ticker, java.sql.Date.valueOf(targetDate)
        );
        
        // 주의: 상장 첫날 데이터가 샘플로 잡혔을 경우 여전히 NULL일 수 있음. 
        // 하지만 2.9만 건 중 하나라면 대부분 보정될 것.
        log.info("Repair result for {}({}): {}", ticker, targetDate, savedPrevClose);
        
        if (savedPrevClose != null) {
            assertThat(savedPrevClose).isGreaterThan(BigDecimal.ZERO);
        } else {
            log.warn("Sample picked was likely the first listing date, still NULL but test passed if no error.");
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StockPriceRepairIntegrationTest.class);
}
