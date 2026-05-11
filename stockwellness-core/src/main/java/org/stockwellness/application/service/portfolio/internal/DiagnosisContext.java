package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

/**
 * 진단에 필요한 원천 데이터 묶음
 */
public record DiagnosisContext(
        Portfolio portfolio,
        Map<String, Stock> stockMap,
        Map<String, List<StockPrice>> stockPriceMap,
        BacktestResult backtestResult,
        Map<String, Map<String, BigDecimal>> correlationMatrix // 추가
) {
}
