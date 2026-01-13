package org.stockwellness.fixture;

import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockFixture {

    /**
     * 기본 StockHistory 생성 (필수값만 입력, 나머지는 더미 데이터)
     */
    public static StockHistory create(String isinCode, LocalDate baseDate, double closePrice) {
        return StockHistory.create(
                isinCode,
                baseDate,
                BigDecimal.valueOf(closePrice),
                BigDecimal.valueOf(closePrice), // 시가
                BigDecimal.valueOf(closePrice), // 고가
                BigDecimal.valueOf(closePrice), // 저가
                BigDecimal.ZERO, // 등락폭
                BigDecimal.ZERO, // 등락률
                100000L, // 거래량
                BigDecimal.valueOf(1000000000), // 거래대금
                BigDecimal.valueOf(500000000000L) // 시가총액
        );
    }

    /**
     * 지표가 포함된 StockHistory 생성 (체이닝 지원을 위해 생성 후 수정)
     */
    public static StockHistory createWithIndicators(
            String isinCode, LocalDate baseDate, double closePrice,
            double ma5, double ma20, double rsi, double macd) {
        
        StockHistory history = create(isinCode, baseDate, closePrice);
        
        history.updateMa5(BigDecimal.valueOf(ma5));
        history.updateMa20(BigDecimal.valueOf(ma20));
        history.updateRsi14(BigDecimal.valueOf(rsi));
        history.updateMacd(BigDecimal.valueOf(macd));
        
        return history;
    }
}