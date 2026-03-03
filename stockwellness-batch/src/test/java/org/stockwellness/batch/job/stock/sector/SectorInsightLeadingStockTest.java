package org.stockwellness.batch.job.stock.sector;

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
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@SpringBootTest
@ActiveProfiles("realdb")
@AutoConfigureTestDatabase(replace = NONE)
class SectorInsightLeadingStockTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private org.springframework.batch.core.Job sectorEodJob;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("실제 PostgreSQL 통합 테스트: indexCode 매칭을 통해 주도주 반영 확인")
    void leading_stock_code_match_test() throws Exception {
        // 1. Given: 실제 DB에서 유효한 섹터 정보 하나를 가져옴
        String findSql = "SELECT sector_code, base_date FROM sector_insight ORDER BY base_date DESC LIMIT 1";
        Map<String, Object> sample = jdbcTemplate.queryForMap(findSql);
        
        String sectorCode = (String) sample.get("sector_code");
        LocalDate baseDate = ((java.sql.Date) sample.get("base_date")).toLocalDate();
        String dateStr = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("Testing with existing SectorCode: {}, Date: {}", sectorCode, baseDate);

        // 2. Given: sector_medium_name 에 sectorCode 를 직접 넣은 테스트 종목 생성
        String ticker = "LTST" + UUID.randomUUID().toString().substring(0, 4);
        Stock testStock = Stock.of(
                ticker, "KR" + ticker, "주도주테스트", 
                MarketType.KOSPI, Currency.KRW, 
                "Large", sectorCode, "Small", StockStatus.ACTIVE // [핵심] sector_medium_name == sectorCode
        );
        stockRepository.saveAndFlush(testStock);

        // 20% 상승 시세 데이터 생성
        stockPriceRepository.saveAndFlush(StockPrice.of(
                testStock, baseDate,
                new BigDecimal("1000"), new BigDecimal("1200"), new BigDecimal("1000"), new BigDecimal("1200"),
                new BigDecimal("1200"), new BigDecimal("1000"),
                1000000L, new BigDecimal("999999999"),
                TechnicalIndicators.empty()
        ));

        // 3. When: 섹터 배치 실행
        JobParameters params = new JobParametersBuilder()
                .addString("startDate", dateStr)
                .addString("endDate", dateStr)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(sectorEodJob, params);

        // 4. Then: 주도주 반영 확인
        String json = jdbcTemplate.queryForObject(
                "SELECT leading_stocks::text FROM sector_insight WHERE sector_code = ? AND base_date = CAST(? AS date)",
                String.class,
                sectorCode, java.sql.Date.valueOf(baseDate)
        );
        
        log.info("Result JSON for {}: {}", sectorCode, json);
        assertThat(json).isNotNull();
        assertThat(json).contains("주도주테스트");
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SectorInsightLeadingStockTest.class);
}
