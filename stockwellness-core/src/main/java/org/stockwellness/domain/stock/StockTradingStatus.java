package org.stockwellness.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목의 거래 상태 정보 (KIS 마스터, 국내 전용).
 *
 * <p>해외 종목은 {@link #defaultValue()}로 초기화됩니다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTradingStatus {

    /** 거래정지 여부 */
    @Column(name = "is_trading_halt", nullable = false,columnDefinition = "boolean default false")
    private boolean tradingHalt;

    /** 정리매매 여부 (상장폐지 직전 단계) */
    @Column(name = "is_clearing_trade", nullable = false,columnDefinition = "boolean default false")
    private boolean clearingTrade;

    /** 관리종목 여부 */
    @Column(name = "is_administered", nullable = false,columnDefinition = "boolean default false")
    private boolean administered;

    public static StockTradingStatus ofKospi(KospiItem item) {
        return of(
                "Y".equals(item.tradingHalt()),
                "Y".equals(item.clearingTrade()),
                "Y".equals(item.administeredStock())
        );
    }

    public static StockTradingStatus ofKosdaq(KosdaqItem item) {
        return of(
                "Y".equals(item.tradingHalt()),
                "Y".equals(item.clearingTrade()),
                "Y".equals(item.administeredStock())
        );
    }

    /** 해외 종목 기본값 */
    public static StockTradingStatus defaultValue() {
        return of(false, false, false);
    }

    private static StockTradingStatus of(boolean tradingHalt,
                                         boolean clearingTrade,
                                         boolean administered) {
        StockTradingStatus s = new StockTradingStatus();
        s.tradingHalt   = tradingHalt;
        s.clearingTrade = clearingTrade;
        s.administered  = administered;
        return s;
    }
}
