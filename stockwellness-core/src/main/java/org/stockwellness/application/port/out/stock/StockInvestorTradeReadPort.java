package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.domain.stock.price.TradeDirection;

public interface StockInvestorTradeReadPort {
    Optional<LocalDate> latestFinalDate(String quoteScope);

    List<StockSupplyRankingResult> topStocks(LocalDate tradeDate, String quoteScope, String investorType,
            TradeDirection direction, int limit);
}
