package org.stockwellness.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목의 과열/급등 감시 정보 (KIS 마스터, 국내 전용).
 *
 * <p>해외 종목은 {@link #defaultValue()}로 초기화됩니다.
 * {@code investCaution}은 코스닥 전용이며, 코스피 종목은 항상 {@code false}입니다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOverheatStatus {

    /** 단기과열 종목 지정 여부 */
    @Column(name = "is_short_term_overheat", nullable = false,columnDefinition = "boolean default false")
    private boolean shortTermOverheat;

    /** 공매도 과열 종목 지정 여부 */
    @Column(name = "is_short_sell_overheat", nullable = false,columnDefinition = "boolean default false")
    private boolean shortSellOverheat;

    /** 이상급등 종목 지정 여부 */
    @Column(name = "is_abnormal_surge", nullable = false,columnDefinition = "boolean default false")
    private boolean abnormalSurge;

    /**
     * 투자주의 환기종목 여부 (코스닥 전용).
     * 코스피/해외 종목은 항상 {@code false}.
     */
    @Column(name = "is_invest_caution", nullable = false,columnDefinition = "boolean default false")
    private boolean investCaution;

    public static StockOverheatStatus ofKospi(KospiItem item) {
        return of(
                "Y".equals(item.shortTermOverheat()),
                "Y".equals(item.shortSellOverheat()),
                "Y".equals(item.abnormalSurge()),
                false
        );
    }

    public static StockOverheatStatus ofKosdaq(KosdaqItem item) {
        return of(
                "Y".equals(item.shortTermOverheat()),
                "Y".equals(item.shortSellOverheat()),
                "Y".equals(item.abnormalSurge()),
                "Y".equals(item.investCaution())
        );
    }

    /** 해외 종목 기본값 */
    public static StockOverheatStatus defaultValue() {
        return of(false, false, false, false);
    }

    private static StockOverheatStatus of(boolean shortTermOverheat,
                                          boolean shortSellOverheat,
                                          boolean abnormalSurge,
                                          boolean investCaution) {
        StockOverheatStatus s = new StockOverheatStatus();
        s.shortTermOverheat = shortTermOverheat;
        s.shortSellOverheat = shortSellOverheat;
        s.abnormalSurge     = abnormalSurge;
        s.investCaution     = investCaution;
        return s;
    }
}
