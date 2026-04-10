package org.stockwellness.application.port.in.stock.result;

import java.time.LocalDate;
import java.util.List;

public record StockSupplyRankingResponse(
        LocalDate requestedDate,
        LocalDate effectiveDate,
        List<StockSupplyRankingResult> institutionItems,
        List<StockSupplyRankingResult> foreignItems
) {
}
