package org.stockwellness.domain.stock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.global.error.exception.GlobalException;

import static org.stockwellness.global.error.ErrorCode.INVALID_INPUT_VALUE;

/** A complete, source-backed current risk snapshot. No row means not collected. */
@Entity
@Table(name = "stock_risk_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockRiskStatus {
    @Id
    @Column(name = "stock_id", nullable = false, updatable = false)
    private Long stockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockStatus status;

    @Column(name = "is_trading_halt", nullable = false)
    private boolean tradingHalt;
    @Column(name = "is_clearing_trade", nullable = false)
    private boolean clearingTrade;
    @Column(name = "is_administered", nullable = false)
    private boolean administered;
    @Column(name = "market_warning_level", length = 2)
    private String marketWarningLevel;
    @Column(name = "is_warning_notice", nullable = false)
    private boolean warningNotice;
    @Column(name = "is_unfaithful_disclosure", nullable = false)
    private boolean unfaithfulDisclosure;
    @Column(name = "is_backdoor_listing", nullable = false)
    private boolean backdoorListing;
    @Column(name = "is_short_term_overheat", nullable = false)
    private boolean shortTermOverheat;
    @Column(name = "is_short_sell_overheat", nullable = false)
    private boolean shortSellOverheat;
    @Column(name = "is_abnormal_surge", nullable = false)
    private boolean abnormalSurge;
    @Column(name = "is_invest_caution", nullable = false)
    private boolean investCaution;
    @Column(name = "source_api", nullable = false, length = 50)
    private String sourceApi;
    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;
    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static StockRiskStatus completeSnapshot(Long stockId, StockStatus status,
            boolean tradingHalt, boolean clearingTrade, boolean administered, String marketWarningLevel,
            boolean warningNotice, boolean unfaithfulDisclosure, boolean backdoorListing,
            boolean shortTermOverheat, boolean shortSellOverheat, boolean abnormalSurge,
            boolean investCaution, String sourceApi, OffsetDateTime sourceUpdatedAt) {
        if (stockId == null || stockId <= 0 || status == null || sourceApi == null || sourceApi.isBlank()
                || sourceApi.length() > 50 || (marketWarningLevel != null && marketWarningLevel.length() > 2)) {
            throw new GlobalException(INVALID_INPUT_VALUE);
        }
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var result = new StockRiskStatus();
        result.stockId = stockId;
        result.status = status;
        result.tradingHalt = tradingHalt;
        result.clearingTrade = clearingTrade;
        result.administered = administered;
        result.marketWarningLevel = marketWarningLevel;
        result.warningNotice = warningNotice;
        result.unfaithfulDisclosure = unfaithfulDisclosure;
        result.backdoorListing = backdoorListing;
        result.shortTermOverheat = shortTermOverheat;
        result.shortSellOverheat = shortSellOverheat;
        result.abnormalSurge = abnormalSurge;
        result.investCaution = investCaution;
        result.sourceApi = sourceApi;
        result.sourceUpdatedAt = sourceUpdatedAt;
        result.collectedAt = now;
        result.updatedAt = now;
        return result;
    }
}
