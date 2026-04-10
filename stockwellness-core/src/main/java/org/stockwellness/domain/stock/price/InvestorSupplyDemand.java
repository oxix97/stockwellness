package org.stockwellness.domain.stock.price;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InvestorSupplyDemand {

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

    public static InvestorSupplyDemand empty() {
        return new InvestorSupplyDemand(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, 0L, 0L, 0L
        );
    }
}
