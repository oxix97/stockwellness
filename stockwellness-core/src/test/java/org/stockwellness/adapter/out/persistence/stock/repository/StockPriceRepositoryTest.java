package org.stockwellness.adapter.out.persistence.stock.repository;

import jakarta.persistence.EntityManagerFactory;
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
import org.stockwellness.domain.stock.price.TradeDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    @DisplayName("지정일 전체 시세 조회는 Stock 연관을 fetch join으로 초기화한다")
    void findAllByDateWithStock_fetchJoin() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        StockPrice samsungPrice = createStockPrice(samsung, today, BigDecimal.TEN, BigDecimal.ONE);
        StockPrice skPrice = createStockPrice(skHynix, today, BigDecimal.TEN, BigDecimal.ONE);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice));
        stockPriceRepository.flush();

        List<StockPrice> prices = stockPriceRepository.findAllByDateWithStock(today);

        assertThat(prices).hasSize(2);
        assertThat(prices)
                .allSatisfy(price -> assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(price.getStock())).isTrue());
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

    @Test
    @DisplayName("기관 수급 랭킹 BUY 조회는 양수 수량만 포함하고 내림차순으로 정렬한다")
    void findTopInstitutionStocksBySupply_BuyFiltersPositiveOnly() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);
        Stock naver = Stock.of("035420", "KR7035420009", "NAVER", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(naver);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate, new BigDecimal("500"), new BigDecimal("200"), 500L, 200L);
        StockPrice skPrice = createStockPrice(skHynix, baseDate, new BigDecimal("-300"), new BigDecimal("50"), -300L, 50L);
        StockPrice naverPrice = createStockPrice(naver, baseDate, new BigDecimal("100"), new BigDecimal("-100"), 100L, -100L);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice, naverPrice));
        stockPriceRepository.flush();

        var results = stockPriceRepository.findTopInstitutionStocksBySupply(
                baseDate, TradeDirection.BUY, 10
        );

        assertThat(results).extracting("ticker").containsExactly("005930", "035420");
        assertThat(results.get(0).netBuyingQuantity()).isEqualTo(500L);
        assertThat(results.get(1).netBuyingQuantity()).isEqualTo(100L);
        assertThat(results.get(0).netBuyingAmount()).isEqualByComparingTo("500");
        assertThat(results.get(1).netBuyingAmount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("외국인 수급 랭킹 SELL 조회는 음수 수량만 포함하고 오름차순으로 정렬한다")
    void findTopForeignStocksBySupply_SellFiltersNegativeOnly() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);
        Stock naver = Stock.of("035420", "KR7035420009", "NAVER", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(naver);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate, new BigDecimal("-100"), new BigDecimal("-50"), -100L, -50L);
        StockPrice skPrice = createStockPrice(skHynix, baseDate, new BigDecimal("-400"), new BigDecimal("-200"), -400L, -200L);
        StockPrice naverPrice = createStockPrice(naver, baseDate, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice, naverPrice));
        stockPriceRepository.flush();

        var results = stockPriceRepository.findTopForeignStocksBySupply(
                baseDate, TradeDirection.SELL, 10
        );

        assertThat(results).extracting("ticker").containsExactly("000660", "005930");
        assertThat(results).hasSize(2);
        assertThat(results.get(0).netBuyingQuantity()).isEqualTo(-200L);
        assertThat(results.get(1).netBuyingQuantity()).isEqualTo(-50L);
        assertThat(results.get(0).netBuyingAmount()).isEqualByComparingTo("-200");
        assertThat(results.get(1).netBuyingAmount()).isEqualByComparingTo("-50");
    }

    @Test
    @DisplayName("기관과 외국인 수급 랭킹은 채널별 수량 컬럼과 부호 필터 규칙을 따른다")
    void findTopStocksBySupply_UsesChannelSpecificQuantity() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate, new BigDecimal("1"), new BigDecimal("-10"), 500L, -10L);
        StockPrice skPrice = createStockPrice(skHynix, baseDate, new BigDecimal("-50"), new BigDecimal("-1"), -50L, -300L);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice));
        stockPriceRepository.flush();

        var institutionResults = stockPriceRepository.findTopInstitutionStocksBySupply(
                baseDate, TradeDirection.BUY, 10
        );
        var foreignResults = stockPriceRepository.findTopForeignStocksBySupply(
                baseDate, TradeDirection.SELL, 10
        );

        assertThat(institutionResults).extracting("ticker").containsExactly("005930");
        assertThat(institutionResults.get(0).netBuyingQuantity()).isEqualTo(500L);
        assertThat(institutionResults.get(0).netBuyingAmount()).isEqualByComparingTo("1");
        assertThat(foreignResults).extracting("ticker").containsExactly("000660", "005930");
        assertThat(foreignResults).hasSize(2);
        assertThat(foreignResults.get(0).netBuyingQuantity()).isEqualTo(-300L);
        assertThat(foreignResults.get(1).netBuyingQuantity()).isEqualTo(-10L);
        assertThat(foreignResults.get(0).netBuyingAmount()).isEqualByComparingTo("-1");
        assertThat(foreignResults.get(1).netBuyingAmount()).isEqualByComparingTo("-10");
    }

    @Test
    @DisplayName("지정일 이전 또는 당일 기준 최신 적재일을 조회한다")
    void findLatestDateOnOrBefore_Success() {
        LocalDate oldDate = LocalDate.of(2026, 4, 3);
        LocalDate latestDate = LocalDate.of(2026, 4, 7);

        stockPriceRepository.saveAll(List.of(
                createStockPrice(samsung, oldDate, BigDecimal.TEN, BigDecimal.ONE),
                createStockPrice(samsung, latestDate, BigDecimal.TEN, BigDecimal.ONE)
        ));
        stockPriceRepository.flush();

        Optional<LocalDate> result = stockPriceRepository.findLatestDateOnOrBefore(LocalDate.of(2026, 4, 5));

        assertThat(result).contains(oldDate);
    }

    private StockPrice createStockPrice(
            Stock stock,
            LocalDate baseDate,
            BigDecimal netInstitutionalBuyingAmt,
            BigDecimal netForeignBuyingAmt
    ) {
        return createStockPrice(stock, baseDate, netInstitutionalBuyingAmt, netForeignBuyingAmt, 0L, 0L);
    }

    private StockPrice createStockPrice(
            Stock stock,
            LocalDate baseDate,
            BigDecimal netInstitutionalBuyingAmt,
            BigDecimal netForeignBuyingAmt,
            Long netInstitutionalBuyingQty,
            Long netForeignBuyingQty
    ) {
        return StockPrice.of(
                stock,
                baseDate,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(100),
                1000L,
                BigDecimal.valueOf(100000),
                netInstitutionalBuyingAmt,
                netForeignBuyingAmt,
                netInstitutionalBuyingQty,
                netForeignBuyingQty,
                null
        );
    }
}
