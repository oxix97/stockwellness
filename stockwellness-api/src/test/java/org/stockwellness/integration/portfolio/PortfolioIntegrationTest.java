package org.stockwellness.integration.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.stockwellness.adapter.in.web.portfolio.dto.BacktestRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.CreateSimulatedPortfolioRequest;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.RebalancingPeriod;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.integration.common.BaseIntegrationTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class PortfolioIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        memberRepository.deleteAll();
        stockRepository.deleteAll();

        // 사용자 로그인 및 토큰 획득 (자동으로 Member 저장됨)
        accessToken = loginAndGetToken("test@example.com", "tester");

        // 테스트용 종목 생성
        Stock samsung = StockFixture.createSamsung();
        stockRepository.saveAndFlush(samsung);
        Stock skHynix = Stock.of("000660", "KR7000660001", "SK하이닉스", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.saveAndFlush(skHynix);

        // Prices are supplied through the canonical read port mock below.
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        stubCanonicalPriceReads(samsung, skHynix, today, yesterday);
    }

    private void stubCanonicalPriceReads(Stock samsung, Stock skHynix, LocalDate today, LocalDate yesterday) {
        PriceObservation samsungToday = price(samsung.getId(), today, "51500", "50500");
        PriceObservation samsungYesterday = price(samsung.getId(), yesterday, "50500", "50000");
        PriceObservation hynixToday = price(skHynix.getId(), today, "200000", "198000");
        Map<Long, List<PriceObservation>> history = Map.of(
                samsung.getId(), List.of(samsungToday, samsungYesterday),
                skHynix.getId(), List.of(hynixToday));

        given(stockPriceReadPort.latest(anySet(), eq("KRX"), any(), eq(PriceBasis.RAW))).willAnswer(invocation -> {
            Set<Long> stockIds = invocation.getArgument(0);
            Map<Long, PriceObservation> latest = new LinkedHashMap<>();
            stockIds.forEach(id -> {
                List<PriceObservation> rows = history.get(id);
                if (rows != null && !rows.isEmpty()) latest.put(id, rows.getFirst());
            });
            return latest;
        });
        given(stockPriceReadPort.recent(anySet(), eq("KRX"), any(), anyInt(), eq(PriceBasis.RAW)))
                .willAnswer(invocation -> limitHistory(history, invocation.getArgument(0), invocation.getArgument(3)));
        given(stockPriceReadPort.history(anySet(), eq("KRX"), any(), any(), eq(PriceBasis.RAW)))
                .willAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(2);
                    LocalDate end = invocation.getArgument(3);
                    Map<Long, List<PriceObservation>> matching = new LinkedHashMap<>();
                    history.forEach((id, rows) -> matching.put(id, rows.stream()
                            .filter(row -> !row.tradeDate().isBefore(start) && !row.tradeDate().isAfter(end))
                            .sorted(java.util.Comparator.comparing(PriceObservation::tradeDate))
                            .toList()));
                    return matching;
                });

        IndexPriceObservation kospiYesterday = indexPrice(1L, yesterday, "2505", "2500");
        IndexPriceObservation kospiToday = indexPrice(1L, today, "2520", "2505");
        List<IndexPriceObservation> indexHistory = List.of(kospiYesterday, kospiToday);
        given(indexDailyReadPort.historyFinal(eq("0001"), any(), any())).willAnswer(invocation -> {
            LocalDate start = invocation.getArgument(1);
            LocalDate end = invocation.getArgument(2);
            return indexHistory.stream().filter(row -> !row.tradeDate().isBefore(start)
                    && !row.tradeDate().isAfter(end)).toList();
        });
        given(indexDailyReadPort.latestFinal(eq("0001"), any())).willAnswer(invocation -> {
            LocalDate asOf = invocation.getArgument(1);
            return indexHistory.stream().filter(row -> !row.tradeDate().isAfter(asOf))
                    .max(java.util.Comparator.comparing(IndexPriceObservation::tradeDate));
        });
    }

    private static Map<Long, List<PriceObservation>> limitHistory(
            Map<Long, List<PriceObservation>> history, Set<Long> stockIds, int limit) {
        Map<Long, List<PriceObservation>> result = new LinkedHashMap<>();
        stockIds.forEach(id -> result.put(id, history.getOrDefault(id, List.of()).stream()
                .limit(limit).toList()));
        return result;
    }

    private static PriceObservation price(long stockId, LocalDate date, String close, String previousClose) {
        BigDecimal value = new BigDecimal(close);
        return new PriceObservation(stockId, date, "KRX", PriceBasis.RAW, value, value, value, value,
                1000L, BigDecimal.TEN, "FINAL", 1, value, new BigDecimal(previousClose),
                null, null, null, null, null, null, null, null, null, null);
    }

    private static IndexPriceObservation indexPrice(
            long seriesId, LocalDate date, String close, String previousClose) {
        BigDecimal value = new BigDecimal(close);
        return new IndexPriceObservation(seriesId, date, value, value, value, 1, 1, 1, 0, 0, 1,
                "FINAL", value, 1000L, BigDecimal.TEN, new BigDecimal(previousClose));
    }

    @Test
    @DisplayName("포트폴리오 생성 통합 테스트: 생성 후 목록 조회 시 포함되어야 한다")
    void createAndGetPortfolio_Success() throws Exception {
        // given: 포트폴리오 생성 요청
        PortfolioItemRequest itemRequest = new PortfolioItemRequest("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        PortfolioCreateRequest createRequest = new PortfolioCreateRequest("내 통합 포트폴리오", "설명", List.of(itemRequest));

        // when: 생성 요청
        mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S001"));

        // then: 목록 조회 확인
        mockMvc.perform(get("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("내 통합 포트폴리오"));
    }

    @Test
    @DisplayName("가상 포트폴리오 통합 테스트: 공통 EOD 종가로 계산한 양수 수량과 기준일을 저장한다")
    void createSimulatedPortfolio_Success() throws Exception {
        CreateSimulatedPortfolioRequest request = new CreateSimulatedPortfolioRequest(
                "가상 통합 포트폴리오",
                "설명",
                new BigDecimal("10000000"),
                List.of(
                        new CreateSimulatedPortfolioRequest.ItemRequest("005930", new BigDecimal("60")),
                        new CreateSimulatedPortfolioRequest.ItemRequest("000660", new BigDecimal("40"))));

        String response = mockMvc.perform(post("/api/v1/portfolios/simulated")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.asOfDate").value(LocalDate.now().toString()))
                .andReturn().getResponse().getContentAsString();

        Long portfolioId = objectMapper.readTree(response).path("data").path("portfolioId").asLong();
        Portfolio portfolio = portfolioRepository.findAllWithItemsByIdIn(List.of(portfolioId)).getFirst();
        assertThat(portfolio.getItems())
                .extracting(PortfolioItem::getSymbol, PortfolioItem::getQuantity, PortfolioItem::getPurchasePrice, PortfolioItem::getCurrency)
                .containsExactlyInAnyOrder(
                        tuple("005930", new BigDecimal("116.504854"), new BigDecimal("51500"), "KRW"),
                        tuple("000660", new BigDecimal("20.000000"), new BigDecimal("200000"), "KRW"));
        assertThat(portfolio.getItems()).allSatisfy(item -> assertThat(item.getQuantity()).isPositive());
    }

    @Test
    @DisplayName("백테스트 통합 테스트: 생성된 포트폴리오에 대해 백테스트 시뮬레이션을 실행한다")
    void runBacktest_Success() throws Exception {
        // [Given] 1. 포트폴리오 생성
        PortfolioItemRequest itemRequest = new PortfolioItemRequest("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        PortfolioCreateRequest createRequest = new PortfolioCreateRequest("백테스트 포트폴리오", "설명", List.of(itemRequest));

        String createResponse = mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long portfolioId = objectMapper.readTree(createResponse).get("data").asLong();

        // [Given] 2. 백테스트 요청 데이터
        BacktestRequest backtestRequest = new BacktestRequest(
                "LUMP_SUM", BigDecimal.valueOf(10000000), "KOSPI", "ALL", true, RebalancingPeriod.MONTHLY, null
        );

        // [When] 3. 백테스트 실행
        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/analysis/backtest", portfolioId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backtestRequest)))
                .andDo(print())
                // [Then] 4. 결과 검증
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cagr").exists());
    }
}
