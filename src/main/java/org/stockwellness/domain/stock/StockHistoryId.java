package org.stockwellness.domain.stock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockHistoryId implements Serializable {

    // StockHistory.isinCode와 매핑
    private String isinCode;

    // StockHistory.baseDate와 매핑
    private LocalDate baseDate;
}