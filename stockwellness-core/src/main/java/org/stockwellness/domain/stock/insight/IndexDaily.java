package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.global.error.exception.GlobalException;

import static org.stockwellness.global.error.ErrorCode.INVALID_INPUT_VALUE;

@Entity
@Table(name = "index_daily")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndexDaily {
    @EmbeddedId
    private IndexDailyId id;

    @MapsId("indexId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "index_id", nullable = false)
    private MarketIndex index;

    @Column(name = "open_price", precision = 19, scale = 4) private BigDecimal openPrice;
    @Column(name = "high_price", precision = 19, scale = 4) private BigDecimal highPrice;
    @Column(name = "low_price", precision = 19, scale = 4) private BigDecimal lowPrice;
    @Column(name = "close_price", precision = 19, scale = 4) private BigDecimal closePrice;
    private Long volume;
    @Column(name = "trading_amount", precision = 25, scale = 2) private BigDecimal tradingAmount;
    @Column(name = "rising_issue_count") private Integer risingIssueCount;
    @Column(name = "upper_limit_issue_count") private Integer upperLimitIssueCount;
    @Column(name = "steady_issue_count") private Integer steadyIssueCount;
    @Column(name = "falling_issue_count") private Integer fallingIssueCount;
    @Column(name = "lower_limit_issue_count") private Integer lowerLimitIssueCount;
    @Column(name = "foreign_net_amount", precision = 25, scale = 2) private BigDecimal foreignNetAmount;
    @Column(name = "institution_net_amount", precision = 25, scale = 2) private BigDecimal institutionNetAmount;
    @Column(name = "price_source_api", nullable = false, length = 50) private String priceSourceApi;
    @Column(name = "supply_source_api", length = 50) private String supplySourceApi;
    @Enumerated(EnumType.STRING) @Column(name = "price_status", nullable = false, length = 20)
    private BenchmarkDataStatus priceStatus;
    @Enumerated(EnumType.STRING) @Column(name = "supply_status", length = 20)
    private BenchmarkDataStatus supplyStatus;
    @Column(name = "collected_at", nullable = false) private OffsetDateTime collectedAt;
    @Column(name = "supply_collected_at") private OffsetDateTime supplyCollectedAt;
    @Column(name = "corrected_at") private OffsetDateTime correctedAt;
    @Column(name = "data_revision", nullable = false) private int dataRevision;

    public static IndexDaily of(MarketIndex index, LocalDate tradeDate,
            BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
            Long volume, BigDecimal tradingAmount, Integer risingIssueCount, Integer upperLimitIssueCount,
            Integer steadyIssueCount, Integer fallingIssueCount, Integer lowerLimitIssueCount,
            BigDecimal foreignNetAmount, BigDecimal institutionNetAmount,
            String priceSourceApi, String supplySourceApi,
            BenchmarkDataStatus priceStatus, BenchmarkDataStatus supplyStatus) {
        if (index == null || tradeDate == null || !valid(priceSourceApi) || priceStatus == null
                || (priceStatus == BenchmarkDataStatus.FINAL && closePrice == null)
                || (highPrice != null && lowPrice != null && highPrice.compareTo(lowPrice) < 0)
                || (volume != null && volume < 0) || negative(tradingAmount)
                || negativeCount(risingIssueCount) || negativeCount(upperLimitIssueCount)
                || negativeCount(steadyIssueCount) || negativeCount(fallingIssueCount)
                || negativeCount(lowerLimitIssueCount)
                || (supplyStatus == BenchmarkDataStatus.FINAL
                    && (!valid(supplySourceApi) || foreignNetAmount == null || institutionNetAmount == null))
                || (supplyStatus != null && !valid(supplySourceApi))) {
            throw new GlobalException(INVALID_INPUT_VALUE);
        }
        IndexDaily daily = new IndexDaily();
        daily.index = index;
        daily.id = new IndexDailyId(index.getId(), tradeDate);
        daily.openPrice = openPrice;
        daily.highPrice = highPrice;
        daily.lowPrice = lowPrice;
        daily.closePrice = closePrice;
        daily.volume = volume;
        daily.tradingAmount = tradingAmount;
        daily.risingIssueCount = risingIssueCount;
        daily.upperLimitIssueCount = upperLimitIssueCount;
        daily.steadyIssueCount = steadyIssueCount;
        daily.fallingIssueCount = fallingIssueCount;
        daily.lowerLimitIssueCount = lowerLimitIssueCount;
        daily.foreignNetAmount = foreignNetAmount;
        daily.institutionNetAmount = institutionNetAmount;
        daily.priceSourceApi = priceSourceApi;
        daily.supplySourceApi = supplySourceApi;
        daily.priceStatus = priceStatus;
        daily.supplyStatus = supplyStatus;
        daily.collectedAt = OffsetDateTime.now(ZoneOffset.UTC);
        daily.dataRevision = 1;
        return daily;
    }

    public boolean reviseFrom(IndexDaily source, OffsetDateTime now) {
        boolean changed = !same(openPrice, source.openPrice) || !same(highPrice, source.highPrice)
                || !same(lowPrice, source.lowPrice) || !same(closePrice, source.closePrice)
                || !Objects.equals(volume, source.volume) || !same(tradingAmount, source.tradingAmount)
                || !Objects.equals(priceStatus, source.priceStatus)
                || !Objects.equals(priceSourceApi, source.priceSourceApi);
        collectedAt = now;
        if (changed) {
            openPrice = source.openPrice;
            highPrice = source.highPrice;
            lowPrice = source.lowPrice;
            closePrice = source.closePrice;
            volume = source.volume;
            tradingAmount = source.tradingAmount;
            priceStatus = source.priceStatus;
            priceSourceApi = source.priceSourceApi;
            dataRevision++;
            correctedAt = now;
        }
        return changed;
    }

    private static boolean valid(String value) { return value != null && !value.isBlank() && value.length() <= 50; }
    private static boolean same(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }
    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
    private static boolean negativeCount(Integer value) { return value != null && value < 0; }
}
