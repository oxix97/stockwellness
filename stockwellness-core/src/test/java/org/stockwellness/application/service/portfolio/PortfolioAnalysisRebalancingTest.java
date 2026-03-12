package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.portfolio.internal.AnalysisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioAnalysisDataLoader;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private StockPricePort stockPricePort;

    @Mock
    private SimulationDataProvider simulationDataProvider;

    @Test
    @DisplayName("목표 비중 대비 현재 비중의 괴리를 계산하고 매매 가이드 수량을 산출한다")
    void calculate_rebalancing_guide() {
        // [Given]
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "Rebalance Test", "");

        PortfolioItem stockItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD", new BigDecimal("60"));
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("400"), "USD", new BigDecimal("40"));
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
}
