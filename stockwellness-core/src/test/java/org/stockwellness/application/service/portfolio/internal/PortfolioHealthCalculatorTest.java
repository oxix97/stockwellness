package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioHealthCalculatorTest {

    private PortfolioHealthCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PortfolioHealthCalculator();
    }

    @Test
    @DisplayName("포트폴리오 구성에 따라 5대 차원 점수가 정확히 산출되어야 한다")
    void calculateHealthScores() {
        // given: 분산이 잘 된 가상의 포트폴리오 구성
        Portfolio portfolio = mock(Portfolio.class);
        Stock stock1 = mock(Stock.class);
        Stock stock2 = mock(Stock.class);
        
        PortfolioItem item1 = mock(PortfolioItem.class);
        PortfolioItem item2 = mock(PortfolioItem.class);
        PortfolioItem cashItem = mock(PortfolioItem.class);

        when(item1.getSymbol()).thenReturn("005930");
        when(item1.getAssetType()).thenReturn(AssetType.STOCK);
        when(item1.calculatePurchaseAmount()).thenReturn(BigDecimal.valueOf(400000));
        
        when(item2.getSymbol()).thenReturn("AAPL");
        when(item2.getAssetType()).thenReturn(AssetType.STOCK);
        when(item2.calculatePurchaseAmount()).thenReturn(BigDecimal.valueOf(400000));
        
        when(cashItem.getAssetType()).thenReturn(AssetType.CASH);
        when(cashItem.calculatePurchaseAmount()).thenReturn(BigDecimal.valueOf(200000));

        when(portfolio.getItems()).thenReturn(List.of(item1, item2, cashItem));
        when(portfolio.calculateTotalPurchaseAmount()).thenReturn(BigDecimal.valueOf(1000000));

        Map<String, Stock> stockMap = new HashMap<>();
        when(stock1.getMarketType()).thenReturn(MarketType.KOSPI);
        when(stock2.getMarketType()).thenReturn(MarketType.NASDAQ);
        stockMap.put("005930", stock1);
        stockMap.put("AAPL", stock2);

        // 추가 컨텍스트 (백테스트 결과 등 정밀화에 필요한 데이터)
        BacktestResult backtestResult = mock(BacktestResult.class);
        when(backtestResult.cagr()).thenReturn(BigDecimal.valueOf(15.0)); // 15% 수익률
        when(backtestResult.mdd()).thenReturn(BigDecimal.valueOf(10.0)); // 10% 낙폭
        
        DiagnosisContext context = new DiagnosisContext(portfolio, stockMap, Collections.emptyMap(), backtestResult);

        // when
        CalculatedHealth health = calculator.calculate(context);

        // then: 점수들이 0~100 사이여야 함
        assertThat(health.categories().get(DiagnosisCategory.RETURN.getKey())).isBetween(0, 100);
        assertThat(health.categories().get(DiagnosisCategory.STABILITY.getKey())).isBetween(0, 100);
        assertThat(health.categories().get(DiagnosisCategory.DIVERSIFICATION.getKey())).isBetween(0, 100);
        assertThat(health.categories().get(DiagnosisCategory.AGILITY.getKey())).isBetween(0, 100);
        assertThat(health.categories().get(DiagnosisCategory.CASH.getKey())).isBetween(0, 100);
        
        // 특정 값 검증 (정밀화된 로직 기대값)
        // 수익: 15% 수익률 -> 약 75점 이상 기대
        assertThat(health.categories().get(DiagnosisCategory.RETURN.getKey())).isGreaterThanOrEqualTo(70);
    }
}
