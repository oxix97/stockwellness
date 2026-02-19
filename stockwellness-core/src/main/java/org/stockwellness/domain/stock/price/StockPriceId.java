package org.stockwellness.domain.stock.price;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockPriceId implements Serializable {
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "base_date")
    private LocalDate baseDate;
}