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

    @Column(name = "prsn_buying_amt", precision = 25, scale = 2)
    private BigDecimal personAmt;

    @Column(name = "total_net_amt", precision = 25, scale = 2)
    private BigDecimal totalNetAmt;

    // --- 수량 (Quantity, 단위: 주) ---
    @Column(name = "inst_buying_qty")
    private Long institutionQty;

    @Column(name = "frgn_buying_qty")
    private Long foreignQty;

    @Column(name = "prsn_buying_qty")
    private Long personQty;

    @Column(name = "total_net_qty")
    private Long totalNetQty;

    public static InvestorSupplyDemand empty() {
        return new InvestorSupplyDemand(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, 0L
        );
    }
}
