package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SectorDataPort {
    /**
     * 특정 업종 코드의 특정 날짜 상세 지수 정보를 가져옵니다.
     */
    SectorDailySnapshot fetchDailySectorDetail(String indexCode, LocalDate date);

    /**
     * 특정 업종 코드의 특정 날짜 기준 투자자 매매동향을 가져옵니다.
     */
    List<InvestorTradingSnapshot> fetchInvestorTradingDaily(String indexCode, LocalDate date, int days);

    /**
     * 특정 업종 코드의 과거 지수 이력을 가져옵니다. (MA, RSI 계산용)
     */
    List<BigDecimal> fetchHistoricalIndexPrices(String indexCode, LocalDate endDate, int days);

    /**
     * 특정 업종 코드의 당일 상세 스냅샷을 가져옵니다.
     * 원천 응답은 {@code SectorDailyDetail} 저장 후 {@code SectorInsight} 생성의 입력으로만 사용합니다.
     */
    SectorDailyDetailSnapshot fetchTodaySectorDetail(String indexCode, LocalDate date);
}
