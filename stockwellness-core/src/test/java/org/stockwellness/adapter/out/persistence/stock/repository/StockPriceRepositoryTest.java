package org.stockwellness.adapter.out.persistence.stock.repository;

import jakarta.persistence.EntityManager;
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
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.domain.stock.price.TradeDirection;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;

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

    @Autowired
    private EntityManager entityManager;

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
                ti1);

        // 역배열, RSI 70 이상 (필터 제외 대상)
        TechnicalIndicators ti2 = new TechnicalIndicators(
                new BigDecimal("70"), new BigDecimal("80"), new BigDecimal("90"), new BigDecimal("100"),
                new BigDecimal("75.0"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null,
                AlignmentStatus.REVERSE, false, true, false, "Bad"
        );
        StockPrice p2 = StockPrice.of(skHynix, today, BigDecimal.valueOf(80), BigDecimal.valueOf(85), BigDecimal.valueOf(75), 
                BigDecimal.valueOf(82), BigDecimal.valueOf(82), BigDecimal.valueOf(80), 500L, BigDecimal.valueOf(40000), 
                ti2);

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

        StockPrice p1_old = StockPrice.of(samsung, d1, BigDecimal.valueOf(500), BigDecimal.valueOf(510), BigDecimal.valueOf(490), BigDecimal.valueOf(500), BigDecimal.valueOf(500), BigDecimal.valueOf(490), 100L, BigDecimal.ZERO, null);
        StockPrice p1_new = StockPrice.of(samsung, d2, BigDecimal.valueOf(510), BigDecimal.valueOf(520), BigDecimal.valueOf(500), BigDecimal.valueOf(515), BigDecimal.valueOf(515), BigDecimal.valueOf(500), 100L, BigDecimal.ZERO, null);
        StockPrice p2_new = StockPrice.of(skHynix, d2, BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(90), BigDecimal.valueOf(105), BigDecimal.valueOf(105), BigDecimal.valueOf(100), 100L, BigDecimal.ZERO, null);

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
        StockPrice samsungPrice = createStockPrice(samsung, today);
        StockPrice skPrice = createStockPrice(skHynix, today);

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
        
        StockPrice p1 = StockPrice.of(otherStock, d1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, BigDecimal.ZERO, null);
        StockPrice p2 = StockPrice.of(otherStock, d2, BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), 100L, BigDecimal.ZERO, null);
        StockPrice p3 = StockPrice.of(otherStock, d3, BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 100L, BigDecimal.ZERO, null);
        StockPrice p4 = StockPrice.of(otherStock, d4, BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), 100L, BigDecimal.ZERO, null);
        
        stockPriceRepository.saveAll(List.of(p1, p2, p3, p4));
        stockPriceRepository.flush();

        // when: d1(10일) 포함하여 최근 3일치 조회 (lt d1.plusDays(1) -> d1, d2, d3)
        List<StockPrice> recentPrices = stockPriceRepository.findRecentPricesByStocks(List.of(otherStock), d1.plusDays(1), 3);

        // then
        List<LocalDate> dates = recentPrices.stream().map(p -> p.getId().getBaseDate()).toList();
        assertThat(dates).contains(d1, d2, d3);
    }

    @Test
    @DisplayName("단일 종목의 최근 가격 리스트를 limit 개수만큼 조회한다")
    void findRecentPricesByStock_Success() {
        Stock otherStock = Stock.of("ONE", "ISIN_ONE", "단일종목", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(otherStock);

        LocalDate d1 = LocalDate.of(2026, 1, 10);
        LocalDate d2 = LocalDate.of(2026, 1, 9);
        LocalDate d3 = LocalDate.of(2026, 1, 8);
        LocalDate d4 = LocalDate.of(2026, 1, 7);

        stockPriceRepository.saveAll(List.of(
                StockPrice.of(otherStock, d1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, BigDecimal.ZERO, null),
                StockPrice.of(otherStock, d2, BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), 100L, BigDecimal.ZERO, null),
                StockPrice.of(otherStock, d3, BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 100L, BigDecimal.ZERO, null),
                StockPrice.of(otherStock, d4, BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), BigDecimal.valueOf(103), 100L, BigDecimal.ZERO, null)
        ));
        stockPriceRepository.flush();

        List<StockPrice> recentPrices = stockPriceRepository.findRecentPricesByStock(
                otherStock,
                d1.plusDays(1),
                org.springframework.data.domain.PageRequest.of(0, 3)
        );

        assertThat(recentPrices)
                .extracting(price -> price.getId().getBaseDate())
                .containsExactly(d1, d2, d3);
        assertThat(recentPrices)
                .allSatisfy(price -> assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(price.getStock())).isTrue());
    }

    @Test
    @DisplayName("기관 수급 랭킹 BUY 조회는 StockInvestorTrade 금액 기준으로 정렬한다")
    void findTopInstitutionStocksBySupply_BuyFiltersPositiveAmountOnly() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);
        Stock naver = Stock.of("035420", "KR7035420009", "NAVER", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(naver);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate, new BigDecimal("71000"), new BigDecimal("70000"), new BigDecimal("120000000000"));
        StockPrice skPrice = createStockPrice(skHynix, baseDate, new BigDecimal("202000"), new BigDecimal("204000"), new BigDecimal("80000000000"));
        StockPrice naverPrice = createStockPrice(naver, baseDate, new BigDecimal("180000"), new BigDecimal("179000"), new BigDecimal("50000000000"));

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice, naverPrice));
        persistInvestorTrade(StockInvestorTrade.of(
                samsung, baseDate, samsung.getName(), samsung.getTicker(),
                10L, 100L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("50"), new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                skHynix, baseDate, skHynix.getName(), skHynix.getTicker(),
                20L, -50L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("200"), new BigDecimal("-300"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                naver, baseDate, naver.getName(), naver.getTicker(),
                30L, 70L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("300"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        stockPriceRepository.flush();
        entityManager.flush();
        entityManager.clear();

        var results = stockPriceRepository.findTopInstitutionStocksBySupply(
                baseDate, TradeDirection.BUY, 10
        );

        assertThat(results).extracting(StockSupplyRankingResult::ticker)
                .containsExactly("005930", "035420");
        assertThat(results.get(0).netBuyingAmount()).isEqualByComparingTo("500");
        assertThat(results.get(1).netBuyingAmount()).isEqualByComparingTo("100");
        assertThat(results.getFirst().currentPrice()).isEqualByComparingTo("71000");
        assertThat(results.getFirst().transactionAmount()).isEqualByComparingTo("120000000000");
    }

    @Test
    @DisplayName("외국인 수급 랭킹 SELL 조회는 음수 금액만 반환한다")
    void findTopForeignStocksBySupply_SellFiltersNegativeAmountOnly() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);
        Stock naver = Stock.of("035420", "KR7035420009", "NAVER", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.save(naver);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate);
        StockPrice skPrice = createStockPrice(skHynix, baseDate);
        StockPrice naverPrice = createStockPrice(naver, baseDate);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice, naverPrice));
        persistInvestorTrade(StockInvestorTrade.of(
                samsung, baseDate, samsung.getName(), samsung.getTicker(),
                -10L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("-500"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                skHynix, baseDate, skHynix.getName(), skHynix.getTicker(),
                20L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                naver, baseDate, naver.getName(), naver.getTicker(),
                -30L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("-200"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        stockPriceRepository.flush();
        entityManager.flush();
        entityManager.clear();

        var results = stockPriceRepository.findTopForeignStocksBySupply(
                baseDate, TradeDirection.SELL, 10
        );

        assertThat(results).extracting(StockSupplyRankingResult::ticker)
                .containsExactly("005930", "035420");
    }

    @Test
    @DisplayName("최신 수급 기준일은 StockInvestorTrade 기준으로 조회한다")
    void findTopStocksBySupply_UsesChannelSpecificAmount() {
        LocalDate baseDate = LocalDate.of(2026, 4, 7);

        StockPrice samsungPrice = createStockPrice(samsung, baseDate);
        StockPrice skPrice = createStockPrice(skHynix, baseDate);

        stockPriceRepository.saveAll(List.of(samsungPrice, skPrice));
        persistInvestorTrade(StockInvestorTrade.of(
                samsung, baseDate, samsung.getName(), samsung.getTicker(),
                5L, 10L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("150"), new BigDecimal("300"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                skHynix, baseDate.minusDays(1), skHynix.getName(), skHynix.getTicker(),
                6L, 9L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("120"), new BigDecimal("200"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        stockPriceRepository.flush();
        entityManager.flush();

        var latestDate = stockPriceRepository.findLatestInvestorTradeDate();

        assertThat(latestDate).contains(baseDate);
    }

    @Test
    @DisplayName("최신 수급 기준일 조회는 stock_price와 조인 가능한 날짜만 사용한다")
    void findLatestInvestorTradeDate_ignoresUnmatchedInvestorTradeDate() {
        LocalDate matchedDate = LocalDate.of(2026, 4, 10);
        LocalDate unmatchedDate = LocalDate.of(2026, 4, 12);

        stockPriceRepository.save(createStockPrice(samsung, matchedDate));
        persistInvestorTrade(StockInvestorTrade.of(
                samsung, matchedDate, samsung.getName(), samsung.getTicker(),
                5L, 10L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("150"), new BigDecimal("300"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        persistInvestorTrade(StockInvestorTrade.of(
                samsung, unmatchedDate, samsung.getName(), samsung.getTicker(),
                5L, 10L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new BigDecimal("150"), new BigDecimal("300"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        stockPriceRepository.flush();
        entityManager.flush();

        var latestDate = stockPriceRepository.findLatestInvestorTradeDate();

        assertThat(latestDate).contains(matchedDate);
    }

    @Test
    @DisplayName("지정일 이전 또는 당일 기준 최신 적재일을 조회한다")
    void findLatestDateOnOrBefore_Success() {
        LocalDate oldDate = LocalDate.of(2026, 4, 3);
        LocalDate latestDate = LocalDate.of(2026, 4, 7);

        stockPriceRepository.saveAll(List.of(
                createStockPrice(samsung, oldDate),
                createStockPrice(samsung, latestDate)
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
        return createStockPrice(stock, baseDate);
    }

    private StockPrice createStockPrice(
            Stock stock,
            LocalDate baseDate,
            BigDecimal closePrice,
            BigDecimal previousClosePrice,
            BigDecimal transactionAmount
    ) {
        return StockPrice.of(
                stock,
                baseDate,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                closePrice,
                closePrice,
                previousClosePrice,
                1000L,
                transactionAmount,
                null
        );
    }

    private StockPrice createStockPrice(
            Stock stock,
            LocalDate baseDate
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
                null
        );
    }

    private void persistInvestorTrade(StockInvestorTrade trade) {
        entityManager.persist(trade);
    }
}
