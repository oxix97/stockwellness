package org.stockwellness.domain.stock.price;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EodStockInvestorTradeId implements Serializable {
    @Column(name = "stock_id", nullable = false) private Long stockId;
    @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;
    @Column(name = "quote_scope", nullable = false, length = 10) private String quoteScope;
}
