package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.Stock;

/** Source-specific, scoped investor flow. Negative net flows are valid. */
@Entity
@Table(name = "stock_investor_trade_eod")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EodStockInvestorTrade {
    @EmbeddedId private EodStockInvestorTradeId id;
    @MapsId("stockId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    @Column(name = "foreign_net_quantity") private Long foreignNetQuantity;
    @Column(name = "institution_net_quantity") private Long institutionNetQuantity;
    @Column(name = "foreign_net_amount", precision = 25, scale = 2) private BigDecimal foreignNetAmount;
    @Column(name = "institution_net_amount", precision = 25, scale = 2) private BigDecimal institutionNetAmount;
    @Column(name = "data_status", nullable = false, length = 20) private String dataStatus;
    @Column(name = "source_api", nullable = false, length = 50) private String sourceApi;
    @Column(name = "collected_at", nullable = false) private Instant collectedAt;
    @Column(name = "corrected_at") private Instant correctedAt;
    @Column(name = "data_revision", nullable = false) private Integer dataRevision;
}
