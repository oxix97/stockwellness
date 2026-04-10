package org.stockwellness.domain.stock.price;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(name = "stock_investor_trade", indexes = {
        @Index(name = "idx_investor_trade_lookup", columnList = "stock_id, base_date DESC")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInvestorTrade {

    @EmbeddedId
    private StockPriceId id;

    @MapsId("stockId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    // --- 금액 (Amount, 단위: 원) ---
    @Column(name = "inst_buying_amt", precision = 25, scale = 2)
    private BigDecimal institutionAmt;

    @Column(name = "frgn_buying_amt", precision = 25, scale = 2)
    private BigDecimal foreignAmt;

    @Column(name = "pension_buying_amt", precision = 25, scale = 2)
    private BigDecimal pensionFundAmt;

    @Column(name = "trust_buying_amt", precision = 25, scale = 2)
    private BigDecimal trustAmt;

    @Column(name = "etc_corp_buying_amt", precision = 25, scale = 2)
    private BigDecimal etcCorpAmt;

    @Column(name = "total_net_amt", precision = 25, scale = 2)
    private BigDecimal totalNetAmt;

    // --- 수량 (Quantity, 단위: 주) ---
    @Column(name = "inst_buying_qty")
    private Long institutionQty;

    @Column(name = "frgn_buying_qty")
    private Long foreignQty;

    @Column(name = "pension_buying_qty")
    private Long pensionFundQty;

    @Column(name = "trust_buying_qty")
    private Long trustQty;

    @Column(name = "etc_corp_buying_qty")
    private Long etcCorpQty;

    @Column(name = "total_net_qty")
    private Long totalNetQty;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static StockInvestorTrade of(Stock stock, LocalDate baseDate,
                                        BigDecimal instAmt, BigDecimal frgnAmt, BigDecimal pensionAmt,
                                        BigDecimal trustAmt, BigDecimal etcCorpAmt, BigDecimal totalAmt,
                                        Long instQty, Long frgnQty, Long pensionQty,
                                        Long trustQty, Long etcCorpQty, Long totalQty) {
        var entity = new StockInvestorTrade();
        entity.id = new StockPriceId(baseDate, stock.getId());
        entity.stock = stock;
        entity.institutionAmt = instAmt;
        entity.foreignAmt = frgnAmt;
        entity.pensionFundAmt = pensionAmt;
        entity.trustAmt = trustAmt;
        entity.etcCorpAmt = etcCorpAmt;
        entity.totalNetAmt = totalAmt;
        entity.institutionQty = instQty;
        entity.foreignQty = frgnQty;
        entity.pensionFundQty = pensionQty;
        entity.trustQty = trustQty;
        entity.etcCorpQty = etcCorpQty;
        entity.totalNetQty = totalQty;
        return entity;
    }
}
