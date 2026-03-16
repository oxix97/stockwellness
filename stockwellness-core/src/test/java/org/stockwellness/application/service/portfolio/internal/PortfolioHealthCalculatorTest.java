package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.TestEntityFactory;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioHealthCalculatorTest {

    private PortfolioHealthCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PortfolioHealthCalculator();
    }

    @Test
    void testCalculateHealthWithRealData() {
        // given
        Portfolio portfolio = TestEntityFactory.createPortfolio(1L, "Test Portfolio");

        // 1. 삼성전자 (공격적 설정)
        Stock samsung = TestEntityFactory.createStock("005930", "삼성전자", MarketType.KOSPI);
        PortfolioItem item1 = TestEntityFactory.createStockItem("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(70000));

        // 2. 현금 (안전 자산)
        PortfolioItem item2 = TestEntityFactory.createCashItem(BigDecimal.valueOf(300000));
        
        portfolio.updateItems(List.of(item1, item2));

        // 삼성전자 가상 주가 (우상향 데이터)
        List<StockPrice> prices1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            prices1.add(TestEntityFactory.createStockPrice(samsung, LocalDate.now().minusDays(10 - i), BigDecimal.valueOf(70000 + (i * 1000))));
        }

        Map<String, Stock> stockMap = Map.of("005930", samsung);
        Map<String, List<StockPrice>> stockPriceMap = Map.of("005930", prices1);

        DiagnosisContext context = new DiagnosisContext(portfolio, stockMap, stockPriceMap);

        // when
        CalculatedHealth result = calculator.calculate(context);

        // then
        assertThat(result.overallScore()).isGreaterThan(0);
        assertThat(result.categories()).containsKeys("defense", "attack", "endurance", "balance");
        assertThat(result.riskMetrics().sharpeRatio()).isNotNull();
        assertThat(result.riskMetrics().mdd()).isNotNull();
        
        System.out.println("Overall Score: " + result.overallScore());
        System.out.println("Risk Metrics: " + result.riskMetrics());
    }
}
