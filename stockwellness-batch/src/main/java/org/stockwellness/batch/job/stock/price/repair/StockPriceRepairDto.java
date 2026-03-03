package org.stockwellness.batch.job.stock.price.repair;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시세 데이터 보정(Repair)을 위해 최소한의 정보만 담는 DTO
 */
public record StockPriceRepairDto(
        Long stockId,
        LocalDate baseDate,
        BigDecimal calculatedPrevClose
) {
}
