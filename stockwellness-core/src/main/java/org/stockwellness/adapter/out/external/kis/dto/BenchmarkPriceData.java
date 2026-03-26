package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 국내/해외 지수 시세 데이터를 공용으로 처리하기 위한 인터페이스
 */
public interface BenchmarkPriceData {
    LocalDate baseDate();
    BigDecimal openPrice();
    BigDecimal highPrice();
    BigDecimal lowPrice();
    BigDecimal closePrice();
    BigDecimal prdyVrss();
    BigDecimal prdyCtrt();
    Long volume();
}
