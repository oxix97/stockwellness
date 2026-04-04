package org.stockwellness.adapter.out.persistence.stock.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@DisplayName("StockPriceRepository 통합 테스트 (QueryDSL 포함)")
class StockPriceRepositoryTest {

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private StockRepository stockRepository;

    private Stock samsung;
    private Stock skHynix;

    @BeforeEach
    void setUp() {
        samsung = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        skHynix = Stock.of("000660", "KR7000660001", "SK하이닉스", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.saveAll(List.of(samsung, skHynix));
        stockRepository.flush();
    }

    @Test
    @DisplayName("기술적 지표를 기반으로 종목을 필터링한다")
    void findFilteredStocksByIndicators_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 정배열, RSI 30~70 사이
        TechnicalIndicators ti1 = new TechnicalIndicators(
                new BigDecimal("100"), new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("70"),
                new BigDecimal("50.0"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null,
                AlignmentStatus.PERFECT, true, false, false, "Good"
        );
        StockPrice p1 = StockPrice.of(samsung, today, BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(90), 
                BigDecimal.valueOf(105), BigDecimal.valueOf(105), BigDecimal.valueOf(100), 1000L, BigDecimal.valueOf(100000), 
                BigDecimal.ZERO, BigDecimal.ZERO, ti1);

        // 역배열, RSI 70 이상 (필터 제외 대상)
        TechnicalIndicators ti2 = new TechnicalIndicators(
                new BigDecimal("70"), new BigDecimal("80"), new BigDecimal("90"), new BigDecimal("100"),
                new BigDecimal("75.0"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null,
                AlignmentStatus.REVERSE, false, true, false, "Bad"
        );
        StockPrice p2 = StockPrice.of(skHynix, today, BigDecimal.valueOf(80), BigDecimal.valueOf(85), BigDecimal.valueOf(75), 
                BigDecimal.valueOf(82), BigDecimal.valueOf(82), BigDecimal.valueOf(80), 500L, BigDecimal.valueOf(40000), 
                BigDecimal.ZERO, BigDecimal.ZERO, ti2);

        stockPriceRepository.saveAll(List.of(p1, p2));
        stockPriceRepository.flush();

        // when: 정배열인 종목 필터링
        List<StockPrice> filtered = stockPriceRepository.findFilteredStocksByIndicators(
                today, AlignmentStatus.PERFECT, new BigDecimal("30"), new BigDecimal("70"), null
        );

        // then
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getStock().getTicker()).isEqualTo("005930");
    }

    @Test
    @DisplayName("여러 티커의 가장 최신 종가를 한 번에 조회한다 (N+1 방지)")
    void findLatestPricesByTickers_Success() {
        // given
        LocalDate d1 = LocalDate.now().minusDays(1);
        LocalDate d2 = LocalDate.now();

        StockPrice p1_old = StockPrice.of(samsung, d1, BigDecimal.valueOf(500), BigDecimal.valueOf(510), BigDecimal.valueOf(490), BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.valueOf(490), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        StockPrice p1_new = StockPrice.of(samsung, d2, BigDecimal.valueOf(510), BigDecimal.valueOf(520), BigDecimal.valueOf(500), BigDecimal.valueOf(515), BigDecimal.valueOf(515), BigDecimal.valueOf(500), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        StockPrice p2_new = StockPrice.of(skHynix, d2, BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(90), BigDecimal.valueOf(105), BigDecimal.valueOf(105), BigDecimal.valueOf(100), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);

        stockPriceRepository.saveAll(List.of(p1_old, p1_new, p2_new));
        stockPriceRepository.flush();

        // when
        Map<String, BigDecimal> latestPrices = stockPriceRepository.findLatestPricesByTickers(List.of("005930", "000660"));

        // then
        assertThat(latestPrices).hasSize(2);
        assertThat(latestPrices.get("005930")).isEqualByComparingTo("515");
        assertThat(latestPrices.get("000660")).isEqualByComparingTo("105");
    }

    @Test
    @DisplayName("여러 종목의 최근 가격 리스트를 조회한다")
    void findRecentPricesByStocks_Success() {
        // given
        Stock otherStock = Stock.of("RECENT", "ISIN_RECENT", "최근종목", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(otherStock);
        
        LocalDate d1 = LocalDate.of(2026, 1, 10);
        LocalDate d2 = LocalDate.of(2026, 1, 9);
        LocalDate d3 = LocalDate.of(2026, 1, 8);
        LocalDate d4 = LocalDate.of(2026, 1, 7);
        
        StockPrice p1 = StockPrice.of(otherStock, d1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        StockPrice p2 = StockPrice.of(otherStock, d2, BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        StockPrice p3 = StockPrice.of(otherStock, d3, BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        StockPrice p4 = StockPrice.of(otherStock, d4, BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), 100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        
        stockPriceRepository.saveAll(List.of(p1, p2, p3, p4));
        stockPriceRepository.flush();

        // when: d1(10일) 포함하여 최근 3일치 조회 (lt d1.plusDays(1) -> d1, d2, d3)
        List<StockPrice> recentPrices = stockPriceRepository.findRecentPricesByStocks(List.of(otherStock), d1.plusDays(1), 3);

        // then
        List<LocalDate> dates = recentPrices.stream().map(p -> p.getId().getBaseDate()).toList();
        assertThat(dates).contains(d1, d2, d3);
    }
}
