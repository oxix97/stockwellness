package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
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
@DisplayName("PortfolioRebalancingService 단위 테스트")
class PortfolioRebalancingServiceTest {

    @InjectMocks
    private PortfolioRebalancingService portfolioRebalancingService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPricePort stockPricePort;

    @Test
    @DisplayName("목표 비중 대비 현재 비중의 괴리를 계산하고 매매 가이드 수량을 산출한다")
    void calculate_rebalancing_guide() {
        // [Given]
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "Rebalance Test", "");

        // AAPL: 현재 10주, 매입가 150, 목표 비중 60%
        PortfolioItem stockItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD", new BigDecimal("60"));
        // CASH: 현재 400, 목표 비중 40%
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("400"), "USD", new BigDecimal("40"));
        portfolio.updateItems(List.of(stockItem, cashItem));

        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));

        // AAPL 현재가: 160
        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        given(stockPricePort.loadRecentHistoriesBatch(List.of("AAPL"), 1))
                .willReturn(Map.of("AAPL", List.of(aaplPrice)));

        // [계산 예상치]
        // 현재 가치: (10 * 160) + 400 = 1600 + 400 = 2000
        // 목표 가치 (AAPL): 2000 * 0.6 = 1200
        // 목표 가치 (CASH): 2000 * 0.4 = 800
        // AAPL 차이: 1200 - 1600 = -400 (즉, 400만큼 매도 필요)
        // AAPL 매도 수량: 400 / 160 = 2.5주 매도

        // [When]
        PortfolioRebalancingResult result = portfolioRebalancingService.getRebalancingGuide(memberId, portfolioId);

        // [Then]
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("2000"));
        
        PortfolioRebalancingResult.RebalancingItem stockGuide = result.items().stream()
                .filter(i -> i.symbol().equals("AAPL")).findFirst().get();
        
        assertThat(stockGuide.currentWeight()).isEqualByComparingTo(new BigDecimal("80")); // 1600 / 2000
        assertThat(stockGuide.targetWeight()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(stockGuide.diffWeight()).isEqualByComparingTo(new BigDecimal("-20"));
        assertThat(stockGuide.recommendedQuantity()).isEqualByComparingTo(new BigDecimal("-2.5")); // 2.5주 매도
    }
}
