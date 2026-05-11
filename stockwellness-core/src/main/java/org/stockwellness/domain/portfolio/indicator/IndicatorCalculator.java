package org.stockwellness.domain.portfolio.indicator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.stockwellness.domain.portfolio.vo.ReturnSeries;

/**
 * 포트폴리오 성과 지표 계산을 위한 공통 인터페이스
 */
public interface IndicatorCalculator<T> {
    
    /**
     * 지표를 계산합니다.
     */
    T calculate(IndicatorContext context);

    /**
     * 계산에 필요한 공통 데이터를 담는 컨텍스트
     */
    record IndicatorContext(
        ReturnSeries portfolioReturns,
        List<BigDecimal> dailyValues,
        BigDecimal initialAmount,
        BigDecimal finalAmount,
        double years,
        Map<String, ReturnSeries> benchmarkReturns,
        String primaryBenchmarkTicker,
        BigDecimal riskFreeRate,
        BigDecimal portfolioCagr
    ) {}
}
