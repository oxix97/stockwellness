package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.price.StockInvestorTrade;
import java.util.List;

public interface StockInvestorTradePort {
    void saveAll(List<StockInvestorTrade> trades);
}
