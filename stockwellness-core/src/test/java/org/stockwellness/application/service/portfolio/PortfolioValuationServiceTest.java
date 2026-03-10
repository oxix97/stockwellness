package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.fixture.PortfolioFixture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioValuationService 단위 테스트")
class PortfolioValuationServiceTest {

    @InjectMocks
    private PortfolioValuationService portfolioValuationService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPricePort stockPricePort;

    @Test
    @DisplayName("포트폴리오의 실시간 가치와 성과를 계산할 수 있다")
    void calculate_portfolio_valuation() {
        // given
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Desc");
        
        // AAPL: 10주, 매입가 150
        PortfolioItem stockItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD");
        // 현금: 500
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("500"), "USD");
        portfolio.updateItems(List.of(stockItem, cashItem));

        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));

        // AAPL 시세: 현재가 160, 전일종가 155
        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        given(aaplPrice.getPreviousClosePrice()).willReturn(new BigDecimal("155"));
        
        given(stockPricePort.loadRecentHistoriesBatch(List.of("AAPL"), 1))
                .willReturn(Map.of("AAPL", List.of(aaplPrice)));

        // when
        PortfolioValuationResult result = portfolioValuationService.getValuation(memberId, portfolioId);

        // then
        // Total Purchase: 10 * 150 + 500 = 2000
        assertThat(result.totalPurchaseAmount()).isEqualByComparingTo(new BigDecimal("2000"));
        
        // Current Total: 10 * 160 + 500 = 2100
        assertThat(result.currentTotalValue()).isEqualByComparingTo(new BigDecimal("2100"));
        
        // Total P/L: 2100 - 2000 = 100
        assertThat(result.totalProfitLoss()).isEqualByComparingTo(new BigDecimal("100"));
        
        // Total Return Rate: (100 / 2000) * 100 = 5%
        assertThat(result.totalReturnRate()).isEqualByComparingTo(new BigDecimal("5"));
        
        // Daily P/L: (160 - 155) * 10 = 50
        assertThat(result.dailyProfitLoss()).isEqualByComparingTo(new BigDecimal("50"));
        
        // Previous Total: 155 * 10 + 500 = 2050
        // Daily Return Rate: (50 / 2050) * 100 = 2.4390...
        assertThat(result.dailyReturnRate()).isEqualByComparingTo(new BigDecimal("2.4390"));
    }
}
