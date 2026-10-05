package org.stockwellness.domain.stock.insight;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class IndexDailyId implements Serializable {
    @Column(name = "index_id", nullable = false)
    private Long indexId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
}
