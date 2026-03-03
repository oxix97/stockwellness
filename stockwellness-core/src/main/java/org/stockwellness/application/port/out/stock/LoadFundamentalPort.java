package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 역사적 펀더멘털 데이터(P/E, P/B 등) 조회를 위한 출력 포트
 */
public interface LoadFundamentalPort {

    /**
     * 특정 종목의 날짜별 P/E 비율 맵을 반환합니다.
     */
    Map<LocalDate, BigDecimal> loadHistoricalPeRatios(String ticker, LocalDate start, LocalDate end);

    /**
     * 특정 종목의 날짜별 P/B 비율 맵을 반환합니다.
     */
    Map<LocalDate, BigDecimal> loadHistoricalPbRatios(String ticker, LocalDate start, LocalDate end);
}
