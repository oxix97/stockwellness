package org.stockwellness.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목의 시장 경보 정보 (KIS 마스터, 국내 전용).
 *
 * <p>해외 종목은 {@link #defaultValue()}로 초기화됩니다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockWarningStatus {

    /**
     * 시장경고 단계
     * <ul>
     *   <li>{@code "00"} = 정상</li>
     *   <li>{@code "01"} = 주의</li>
     *   <li>{@code "02"} = 경고</li>
     *   <li>{@code "03"} = 위험예고</li>
     * </ul>
     */
    @Column(name = "market_warning_level", length = 2)
    private String marketWarningLevel;

    /**
     * 경고예고 여부
     */
    @Column(name = "is_warning_notice", nullable = false, columnDefinition = "boolean default false")
    private boolean warningNotice;

    /**
     * 불성실공시 법인 지정 여부
     */
    @Column(name = "is_unfaithful_disclosure", nullable = false,columnDefinition = "boolean default false")
    private boolean unfaithfulDisclosure;

    /**
     * 우회상장 여부
     */
    @Column(name = "is_backdoor_listing", nullable = false,columnDefinition = "boolean default false")
    private boolean backdoorListing;

    public static StockWarningStatus ofKospi(KospiItem item) {
        return of(
                item.marketWarningLevel(),
                "Y".equals(item.warningNotice()),
                "Y".equals(item.unfaithfulDisclosure()),
                "Y".equals(item.backdoorListing())
        );
    }

    public static StockWarningStatus ofKosdaq(KosdaqItem item) {
        return of(
                item.marketWarningLevel(),
                "Y".equals(item.warningNotice()),
                "Y".equals(item.unfaithfulDisclosure()),
                "Y".equals(item.backdoorListing())
        );
    }

    /**
     * 해외 종목 기본값
     */
    public static StockWarningStatus defaultValue() {
        return of(null, false, false, false);
    }

    private static StockWarningStatus of(String marketWarningLevel,
                                         boolean warningNotice,
                                         boolean unfaithfulDisclosure,
                                         boolean backdoorListing) {
        StockWarningStatus s = new StockWarningStatus();
        s.marketWarningLevel = marketWarningLevel;
        s.warningNotice = warningNotice;
        s.unfaithfulDisclosure = unfaithfulDisclosure;
        s.backdoorListing = backdoorListing;
        return s;
    }
}
