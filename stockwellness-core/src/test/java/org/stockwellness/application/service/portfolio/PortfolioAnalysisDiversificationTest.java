package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.portfolio.internal.AnalysisContext;
import org.stockwellness.application.service.portfolio.internal.PortfolioAnalysisDataLoader;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalysisService Diversification 단위 테스트")
class PortfolioAnalysisDiversificationTest {

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    @Mock
    private PortfolioAnalysisDataLoader dataLoader;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private SimulationDataProvider simulationDataProvider;

    @Test
    @DisplayName("포트폴리오의 업종별, 국가별, 자산군별 비중을 계산할 수 있다")
    void calculate_diversification_metrics() {
        // given
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Desc");

        PortfolioItem aaplItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD");
        PortfolioItem samItem = PortfolioItem.createStock("005930", new BigDecimal("10"), new BigDecimal("70000"), "KRW");
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("300000"), "KRW");
        
        portfolio.updateItems(List.of(aaplItem, samItem, cashItem));

        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        StockPrice samPrice = mock(StockPrice.class);
        given(samPrice.getClosePrice()).willReturn(new BigDecimal("72000"));
        Map<String, List<StockPrice>> priceMap = Map.of("AAPL", List.of(aaplPrice), "005930", List.of(samPrice));

        Stock aaplStock = Stock.of("AAPL", "ISIN1", "Apple", MarketType.NASDAQ, Currency.USD, StockSector.of(null, null, null, "IT"), StockStatus.ACTIVE);
        Stock samStock = Stock.of("005930", "ISIN2", "Samsung", MarketType.KOSPI, Currency.KRW, StockSector.of(null, null, null, "전자"), StockStatus.ACTIVE);
        Map<String, Stock> stockMap = Map.of("AAPL", aaplStock, "005930", samStock);

        // DataLoader 모킹 설정
        AnalysisContext context = new AnalysisContext(portfolio, stockMap, priceMap, null);
        given(dataLoader.loadContext(portfolioId, memberId)).willReturn(context);

        // when
        PortfolioDiversificationResult result = portfolioAnalysisService.getDiversification(memberId, portfolioId);

        // then
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("1021600"));
        assertThat(result.assetRatios().get("STOCK")).isEqualByComparingTo(new BigDecimal("70.6343"));
        assertThat(result.assetRatios().get("CASH")).isEqualByComparingTo(new BigDecimal("29.3657"));
        assertThat(result.sectorRatios().get("IT")).isEqualByComparingTo(new BigDecimal("0.1566"));
        assertThat(result.sectorRatios().get("전자")).isEqualByComparingTo(new BigDecimal("70.4777"));
        assertThat(result.countryRatios().get("US")).isEqualByComparingTo(new BigDecimal("0.1566"));
        assertThat(result.countryRatios().get("KR")).isEqualByComparingTo(new BigDecimal("99.8434"));
    }
}
