package org.stockwellness.application.port.in.batch;

import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockPriceRepairUseCase {

    StockPriceRepairResult repair(StockPriceRepairCommand command);

    record StockPriceRepairCommand(
            Stock stock,
            String startDate,
            String endDate
    ) {
    }

    record StockPriceRepairRow(
            Long stockId,
            LocalDate baseDate,
            BigDecimal calculatedPrevClose
    ) {
    }

    record StockPriceRepairResult(List<StockPriceRepairRow> rows) {
    }
}
