package org.stockwellness.adapter.batch.investortradedetail.step.reader;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;

public class StockInvestorTradeDetailReader implements ItemReader<InvestorTradeDetail> {

    private final Iterator<InvestorTradeDetail> iterator;

    public StockInvestorTradeDetailReader(List<InvestorTradeDetail> items) {
        this.iterator = items.iterator();
    }

    @Override
    public InvestorTradeDetail read() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
