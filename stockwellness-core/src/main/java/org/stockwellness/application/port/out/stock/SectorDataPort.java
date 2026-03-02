package org.stockwellness.application.port.out.stock;

import org.stockwellness.adapter.out.external.kis.dto.InvestorTradingDaily;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SectorDataPort {
    /**
     * 특정 업종 코드의 특정 날짜 상세 지수 정보를 가져옵니다.
     */
    KisDailySectorDetail fetchDailySectorDetail(String indexCode, LocalDate date);

    /**
     * 특정 업종 코드의 특정 날짜 기준 투자자 매매동향을 가져옵니다.
     */
    List<InvestorTradingDaily> fetchInvestorTradingDaily(String indexCode, LocalDate date, int days);

    /**
     * 특정 업종 코드의 과거 지수 이력을 가져옵니다. (MA, RSI 계산용)
     */
    List<BigDecimal> fetchHistoricalIndexPrices(String indexCode, LocalDate endDate, int days);

    /**
     * 당일 전체 업종 데이터를 가져옵니다. (배치 Reader 용)
     */
    List<SectorApiDto> fetchTodaySectorData();
}
