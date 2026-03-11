package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.portfolio.internal.AnalysisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioAnalysisDataLoader;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalysisService Valuation 단위 테스트")
class PortfolioAnalysisValuationTest {

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    @Mock
    private PortfolioAnalysisDataLoader dataLoader;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private SimulationDataProvider simulationDataProvider;

    @Test
    @DisplayName("포트폴리오의 실시간 가치와 성과를 계산할 수 있다")
    void calculate_portfolio_valuation() {
        // given
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Desc");
        
        PortfolioItem stockItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD");
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("500"), "USD");
        portfolio.updateItems(List.of(stockItem, cashItem));

        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        given(aaplPrice.getPreviousClosePrice()).willReturn(new BigDecimal("155"));
        
        Map<String, List<StockPrice>> priceMap = Map.of("AAPL", List.of(aaplPrice));
        PortfolioStats mockStats = PortfolioStats.create(portfolio, LocalDate.now(), BigDecimal.valueOf(15.5), BigDecimal.valueOf(1.2), BigDecimal.valueOf(0.95));

        // DataLoader 모킹 설정
        AnalysisContext context = new AnalysisContext(portfolio, Map.of(), priceMap, mockStats);
        given(dataLoader.loadContext(portfolioId, memberId)).willReturn(context);

        // when
        PortfolioValuationResult result = portfolioAnalysisService.getValuation(memberId, portfolioId);

        // then
        assertThat(result.totalPurchaseAmount()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.currentTotalValue()).isEqualByComparingTo(new BigDecimal("2100"));
        assertThat(result.mdd()).isEqualByComparingTo(new BigDecimal("15.5"));
        assertThat(result.sharpeRatio()).isEqualByComparingTo(new BigDecimal("1.2"));
        assertThat(result.beta()).isEqualByComparingTo(new BigDecimal("0.95"));
    }
}
