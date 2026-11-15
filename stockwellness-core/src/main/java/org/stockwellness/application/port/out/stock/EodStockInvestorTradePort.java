package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import org.stockwellness.domain.stock.price.EodStockInvestorTradeDaily;
import org.stockwellness.domain.stock.price.InvestorTradeRanking;
import org.stockwellness.domain.stock.price.InvestorType;

public interface EodStockInvestorTradePort {
    void upsert(List<EodStockInvestorTradeDaily> values);

    List<InvestorTradeRanking> findRanked(LocalDate tradeDate, String quoteScope, InvestorType type, int limit);
}
