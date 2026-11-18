package org.stockwellness.application.service.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.service.portfolio.internal.AnalysisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioAnalysisDataLoader;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.price.StockPrice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalysisService Rebalancing 단위 테스트")
class PortfolioAnalysisRebalancingTest {

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    @Mock
    private PortfolioAnalysisDataLoader dataLoader;

    @Mock
    private SimulationDataProvider simulationDataProvider;

    @Test
    @DisplayName("목표 비중 대비 현재 비중의 괴리를 계산하고 매매 가이드 수량을 산출한다")
    void calculate_rebalancing_guide() {
        // [Given]
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "Rebalance Test", "");

        PortfolioItem stockItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD", new BigDecimal("60"), LocalDate.now());
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("400"), "USD", new BigDecimal("40"), LocalDate.now());
        portfolio.updateItems(List.of(stockItem, cashItem));

        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        Map<String, List<StockPrice>> priceMap = Map.of("AAPL", List.of(aaplPrice));

        // DataLoader 모킹 설정
        AnalysisContext context = new AnalysisContext(portfolio, Map.of(), priceMap, null);
        given(dataLoader.loadContext(portfolioId, memberId)).willReturn(context);

        // [When]
        PortfolioRebalancingResult result = portfolioAnalysisService.getRebalancingGuide(memberId, portfolioId);

        // [Then]
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("2000"));
        
        PortfolioRebalancingResult.RebalancingItem stockGuide = result.items().stream()
                .filter(i -> i.symbol().equals("AAPL")).findFirst().get();
        
        assertThat(stockGuide.currentWeight()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(stockGuide.targetWeight()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(stockGuide.diffWeight()).isEqualByComparingTo(new BigDecimal("-20"));
        assertThat(stockGuide.recommendedQuantity()).isEqualByComparingTo(new BigDecimal("-2.5"));
    }

    @Test
    @DisplayName("0원 종목과 양수 현금 포트폴리오는 0원 종목의 권장 수량을 null로 반환한다")
    void zero_price_stock_with_positive_cash_returns_null_recommended_quantity() {
        Long memberId = 1L;
        Long portfolioId = 101L;
        Portfolio portfolio = Portfolio.create(memberId, "Zero Price + Cash", "");
        PortfolioItem stockItem = PortfolioItem.createStock("964003", BigDecimal.ONE, new BigDecimal("100"), "KRW", new BigDecimal("50"), LocalDate.of(2026, 11, 17));
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("1000"), "KRW", new BigDecimal("50"), LocalDate.of(2026, 11, 17));
        portfolio.updateItems(List.of(stockItem, cashItem));

        StockPrice zeroPrice = mock(StockPrice.class);
        given(zeroPrice.getClosePrice()).willReturn(BigDecimal.ZERO);
        given(dataLoader.loadContext(portfolioId, memberId))
                .willReturn(new AnalysisContext(portfolio, Map.of(), Map.of("964003", List.of(zeroPrice)), null));

        PortfolioRebalancingResult result = portfolioAnalysisService.getRebalancingGuide(memberId, portfolioId);

        assertThat(result.totalValue()).isEqualByComparingTo("1000");
        PortfolioRebalancingResult.RebalancingItem zeroStock = result.items().stream()
                .filter(i -> i.symbol().equals("964003")).findFirst().orElseThrow();
        PortfolioRebalancingResult.RebalancingItem cash = result.items().stream()
                .filter(i -> i.symbol().equals("CASH")).findFirst().orElseThrow();

        assertThat(zeroStock.currentPrice()).isEqualByComparingTo("0");
        assertThat(zeroStock.recommendedQuantity()).isNull();
        assertThat(cash.currentPrice()).isEqualByComparingTo("1");
        assertThat(cash.recommendedQuantity()).isEqualByComparingTo("-500");
    }

    @Test
    @DisplayName("0원 단일 종목 포트폴리오는 총 평가액 0을 유지하되 권장 수량은 null로 반환한다")
    void zero_price_single_stock_returns_null_recommended_quantity() {
        Long memberId = 1L;
        Long portfolioId = 102L;
        Portfolio portfolio = Portfolio.create(memberId, "Single Zero Price", "");
        portfolio.updateItems(List.of(
                PortfolioItem.createStock("964003", BigDecimal.ONE, new BigDecimal("100"), "KRW", new BigDecimal("100"), LocalDate.of(2026, 11, 17))
        ));

        StockPrice zeroPrice = mock(StockPrice.class);
        given(zeroPrice.getClosePrice()).willReturn(BigDecimal.ZERO);
        given(dataLoader.loadContext(portfolioId, memberId))
                .willReturn(new AnalysisContext(portfolio, Map.of(), Map.of("964003", List.of(zeroPrice)), null));

        PortfolioRebalancingResult result = portfolioAnalysisService.getRebalancingGuide(memberId, portfolioId);

        assertThat(result.totalValue()).isEqualByComparingTo("0");
        PortfolioRebalancingResult.RebalancingItem zeroStock = result.items().getFirst();
        assertThat(zeroStock.currentPrice()).isEqualByComparingTo("0");
        assertThat(zeroStock.recommendedQuantity()).isNull();
    }

    @Test
    @DisplayName("양수 현재가에서 목표 비중과 현재 비중이 일치하면 권장 수량은 숫자 0을 반환한다")
    void positive_price_zero_diff_returns_numeric_zero_recommended_quantity() {
        Long memberId = 1L;
        Long portfolioId = 103L;
        Portfolio portfolio = Portfolio.create(memberId, "Exact Weight", "");
        portfolio.updateItems(List.of(
                PortfolioItem.createStock("964001", BigDecimal.TEN, new BigDecimal("100"), "KRW", new BigDecimal("100"), LocalDate.of(2026, 11, 17))
        ));

        StockPrice positivePrice = mock(StockPrice.class);
        given(positivePrice.getClosePrice()).willReturn(new BigDecimal("100"));
        given(dataLoader.loadContext(portfolioId, memberId))
                .willReturn(new AnalysisContext(portfolio, Map.of(), Map.of("964001", List.of(positivePrice)), null));

        PortfolioRebalancingResult result = portfolioAnalysisService.getRebalancingGuide(memberId, portfolioId);

        assertThat(result.totalValue()).isEqualByComparingTo("1000");
        PortfolioRebalancingResult.RebalancingItem item = result.items().getFirst();
        assertThat(item.currentPrice()).isEqualByComparingTo("100");
        assertThat(item.recommendedQuantity()).isNotNull().isEqualByComparingTo("0");
    }
}
