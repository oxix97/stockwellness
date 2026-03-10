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
@DisplayName("PortfolioDiversificationService 단위 테스트")
class PortfolioDiversificationServiceTest {

    @InjectMocks
    private PortfolioDiversificationService portfolioDiversificationService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPort stockPort;

    @Mock
    private StockPricePort stockPricePort;

    @Test
    @DisplayName("포트폴리오의 업종별, 국가별, 자산군별 비중을 계산할 수 있다")
    void calculate_diversification_metrics() {
        // given
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "My Portfolio", "Desc");

        // AAPL (US, IT): 10주, 매입가 150 -> 1500
        PortfolioItem aaplItem = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD");
        // 005930 (KR, 전자): 10주, 매입가 70000 -> 700000
        PortfolioItem samItem = PortfolioItem.createStock("005930", new BigDecimal("10"), new BigDecimal("70000"), "KRW");
        // CASH: 300000
        PortfolioItem cashItem = PortfolioItem.createCash(new BigDecimal("300000"), "KRW");
        
        portfolio.updateItems(List.of(aaplItem, samItem, cashItem));

        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));

        // 시세 (현재가 기준 비중 계산을 위함)
        // AAPL: 160 -> 가치 1600
        StockPrice aaplPrice = mock(StockPrice.class);
        given(aaplPrice.getClosePrice()).willReturn(new BigDecimal("160"));
        // 005930: 72000 -> 가치 720000
        StockPrice samPrice = mock(StockPrice.class);
        given(samPrice.getClosePrice()).willReturn(new BigDecimal("72000"));

        given(stockPricePort.loadRecentHistoriesBatch(List.of("AAPL", "005930"), 1))
                .willReturn(Map.of("AAPL", List.of(aaplPrice), "005930", List.of(samPrice)));

        // 종목 정보 (업종, 시장)
        Stock aaplStock = Stock.of("AAPL", "ISIN1", "Apple", MarketType.NASDAQ, Currency.USD, StockSector.of(null, null, null, "IT"), StockStatus.ACTIVE);
        Stock samStock = Stock.of("005930", "ISIN2", "Samsung", MarketType.KOSPI, Currency.KRW, StockSector.of(null, null, null, "전자"), StockStatus.ACTIVE);

        given(stockPort.loadStocksByTickers(List.of("AAPL", "005930")))
                .willReturn(List.of(aaplStock, samStock));

        // 총 가치 = 1600 + 720000 + 300000 = 1,021,600
        // IT 비중: 1600 / 1,021,600 * 100 = 0.1566...
        // 전자 비중: 720000 / 1,021,600 * 100 = 70.4776...
        // 현금 비중: 300000 / 1,021,600 * 100 = 29.3657...

        // when
        PortfolioDiversificationResult result = portfolioDiversificationService.getDiversification(memberId, portfolioId);

        // then
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("1021600"));
        
        // Asset Type Ratios
        assertThat(result.assetRatios().get("STOCK")).isEqualByComparingTo(new BigDecimal("70.6343")); // (1600+720000)/1021600
        assertThat(result.assetRatios().get("CASH")).isEqualByComparingTo(new BigDecimal("29.3657"));

        // Sector Ratios (Only for Stocks)
        assertThat(result.sectorRatios().get("IT")).isEqualByComparingTo(new BigDecimal("0.1566"));
        assertThat(result.sectorRatios().get("전자")).isEqualByComparingTo(new BigDecimal("70.4777"));

        // Country Ratios
        assertThat(result.countryRatios().get("US")).isEqualByComparingTo(new BigDecimal("0.1566"));
        assertThat(result.countryRatios().get("KR")).isEqualByComparingTo(new BigDecimal("99.8434")); // (720000+300000)/1021600 ? 
        // 아, 현금도 국가가 있을 수 있지만 여기서는 CASH 섹터로 따로 보거나, 통화 기반으로 국가를 매핑할 수 있음.
        // 일단 KRW는 KR, USD는 US로 매핑한다고 가정.
    }
}
