package org.stockwellness.global.util;

import org.stockwellness.domain.stock.Country;
import org.stockwellness.domain.stock.MarketType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 포트폴리오 도메인 매핑 및 그룹화 관련 유틸리티
 */
public class PortfolioMapperUtil {

    /**
     * 시장 타입을 기반으로 국가 코드를 결정합니다.
     */
    public static Country resolveCountry(MarketType marketType) {
        if (marketType == null) return Country.ETC;
        return switch (marketType) {
            case KOSPI, KOSDAQ -> Country.KR;
            case NASDAQ, NYSE, AMEX -> Country.US;
            default -> Country.ETC;
        };
    }

    /**
     * 통화 단위를 기반으로 국가 코드를 결정합니다. (현금 자산용)
     */
    public static Country resolveCountryFromCurrency(String currency) {
        if (currency == null) return Country.ETC;
        return switch (currency.toUpperCase()) {
            case "KRW" -> Country.KR;
            case "USD" -> Country.US;
            case "JPY" -> Country.JP;
            default -> Country.ETC;
        };
    }

    /**
     * 그룹별 합산 가치를 전체 가치 대비 비율(%)로 변환합니다.
     */
    public static Map<String, BigDecimal> calculateRatios(Map<String, BigDecimal> values, BigDecimal total) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return ratios;
        
        values.forEach((key, value) -> 
            ratios.put(key, FinanceCalculationUtil.calculateRate(value, total))
        );
        return ratios;
    }
}
