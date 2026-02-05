package org.stockwellness.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.in.portfolio.result.StockStatResult;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PortfolioDiagnosisServiceTest {

    @InjectMocks
    private PortfolioDiagnosisService portfolioDiagnosisService;

    @Mock
    private StockStatCalculator stockStatCalculator;

    @Mock
    private org.stockwellness.application.port.out.portfolio.LoadPortfolioPort loadPortfolioPort;

    @Mock
    private org.stockwellness.application.port.out.stock.LoadStockPort loadStockPort;

    @Mock
    private org.stockwellness.application.port.out.stock.LoadStockHistoryPort loadStockHistoryPort;

    @Test
    @DisplayName("Should calculate weighted average of scores based on pieceCount")
    void calculateWeightedAverage() {
        // Given
        Long portfolioId = 1L;
        Portfolio portfolio = Portfolio.create(1L, "My Portfolio", "Description");
        PortfolioItem itemA = PortfolioItem.createStock("KR7000020008", 6);
        PortfolioItem itemB = PortfolioItem.createStock("KR7005930003", 2);
        portfolio.updateItems(List.of(itemA, itemB));

        given(loadPortfolioPort.findById(portfolioId)).willReturn(Optional.of(portfolio));

        Stock stockA = Stock.create("KR7000020008", "Stock A", "000020", MarketType.KOSPI, 1000L, null, null);
        Stock stockB = Stock.create("KR7005930003", "Stock B", "005930", MarketType.KOSPI, 1000L, null, null);

        given(loadStockPort.loadStocksByIsinCodes(anyList())).willReturn(List.of(stockA, stockB));

        // Mock histories
        given(loadStockHistoryPort.loadRecentHistoriesBatch(anyList(), any(Integer.class)))
                .willReturn(Map.of("KR7000020008", List.of(), "KR7005930003", List.of()));

        // Stock A scores all 100
        given(stockStatCalculator.calculate(any(), anyList())).willReturn(
                StockStatResult.of("KR7000020008", "Stock A", 100, 100, 100, 100),
                StockStatResult.of("KR7005930003", "Stock B", 50, 50, 50, 50)
        );

        // When
        PortfolioHealthResult health = portfolioDiagnosisService.diagnose(portfolioId);

        // Then
        // (100 * 6 + 50 * 2) / 8 = 87.5 -> round to 88
        assertThat(health.categories().get(DiagnosisCategory.DEFENSE.getKey())).isEqualTo(88);
        assertThat(health.categories().get(DiagnosisCategory.ATTACK.getKey())).isEqualTo(88);
        assertThat(health.categories().get(DiagnosisCategory.ENDURANCE.getKey())).isEqualTo(88);
        assertThat(health.categories().get(DiagnosisCategory.AGILITY.getKey())).isEqualTo(88);
    }

    @Test
    @DisplayName("Should calculate Balance score based on stock count and market dispersion")
    void calculateBalanceScore() {
        // Given
        Long portfolioId = 1L;
        Portfolio portfolio = Portfolio.create(1L, "My Portfolio", "Description");
        // 2 stocks -> Stock Count Score = 60
        // KOSPI + KOSDAQ -> Market Dispersion Score = 100
        // Expected Balance = (60 + 100) / 2 = 80
        PortfolioItem itemA = PortfolioItem.createStock("KR7000020008", 4);
        PortfolioItem itemB = PortfolioItem.createStock("KR7005930003", 4);
        portfolio.updateItems(List.of(itemA, itemB));

        given(loadPortfolioPort.findById(portfolioId)).willReturn(Optional.of(portfolio));

        Stock stockA = Stock.create("KR7000020008", "Stock A", "000020", MarketType.KOSPI, 1000L, null, null);
        Stock stockB = Stock.create("KR7005930003", "Stock B", "005930", MarketType.KOSDAQ, 1000L, null, null);

        given(loadStockPort.loadStocksByIsinCodes(anyList())).willReturn(List.of(stockA, stockB));

        given(loadStockHistoryPort.loadRecentHistoriesBatch(anyList(), any(Integer.class)))
                .willReturn(Map.of("KR7000020008", List.of(), "KR7005930003", List.of()));
        given(stockStatCalculator.calculate(any(), anyList())).willReturn(
                StockStatResult.of("KR7000020008", "Stock A", 50, 50, 50, 50),
                StockStatResult.of("KR7005930003", "Stock B", 50, 50, 50, 50)
        );

        // When
        PortfolioHealthResult health = portfolioDiagnosisService.diagnose(portfolioId);

        // Then
        assertThat(health.categories().get(DiagnosisCategory.BALANCE.getKey())).isEqualTo(80);
    }
}