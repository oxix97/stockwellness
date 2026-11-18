package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "index_series", indexes = {
        @Index(name = "idx_index_series_index", columnList = "index_id"),
        @Index(name = "idx_index_series_calendar_market", columnList = "calendar_market_code")
}, uniqueConstraints = @UniqueConstraint(name = "uq_index_series_provider_identity",
        columnNames = {"provider", "provider_namespace", "provider_series_code", "return_basis", "dividend_basis", "currency"}))
public class IndexSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "index_id", nullable = false)
    private MarketIndex marketIndex;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_namespace", nullable = false, length = 80)
    private String providerNamespace;

    @Column(name = "provider_series_code", nullable = false, length = 100)
    private String providerSeriesCode;

    @Column(name = "calendar_market_code", nullable = false, length = 20)
    private String calendarMarketCode;

    @Column(name = "return_basis", nullable = false, length = 20)
    private String returnBasis;

    @Column(name = "dividend_basis", nullable = false, length = 10)
    private String dividendBasis;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_canonical", nullable = false)
    private boolean canonical;

    public IndexSeries(MarketIndex marketIndex, String provider, String providerNamespace,
                       String providerSeriesCode, String calendarMarketCode, String returnBasis,
                       String dividendBasis, String currency, boolean canonical) {
        if (marketIndex == null || isBlank(provider) || isBlank(providerNamespace) || isBlank(providerSeriesCode)
                || isBlank(calendarMarketCode) || isBlank(currency)) {
            throw new IllegalArgumentException("Index series identity and calendar market are required");
        }
        if (!("PRICE".equals(returnBasis) && "NONE".equals(dividendBasis))
                && !("TOTAL_RETURN".equals(returnBasis)
                && ("GROSS".equals(dividendBasis) || "NET".equals(dividendBasis)))) {
            throw new IllegalArgumentException("Unsupported index return/dividend basis");
        }
        this.marketIndex = marketIndex;
        this.provider = provider;
        this.providerNamespace = providerNamespace;
        this.providerSeriesCode = providerSeriesCode;
        this.calendarMarketCode = calendarMarketCode;
        this.returnBasis = returnBasis;
        this.dividendBasis = dividendBasis;
        this.currency = currency;
        this.canonical = canonical;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
