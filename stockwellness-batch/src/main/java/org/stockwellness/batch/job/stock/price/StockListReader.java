package org.stockwellness.batch.job.stock.price;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.stockwellness.domain.stock.Stock;

import java.util.ArrayList;
import java.util.List;

/**
 * 기존 ItemReader의 결과물을 지정된 크기(size)만큼 리스트로 묶어서 반환함.
 * ItemStreamReader를 구현하여 Delegate의 상태(State) 관리를 보장함.
 */
@Slf4j
public class StockListReader implements ItemStreamReader<List<Stock>> {

    private final ItemStreamReader<Stock> delegate;
    private final int size;

    public StockListReader(ItemStreamReader<Stock> delegate, int size) {
        this.delegate = delegate;
        this.size = size;
    }

    @Override
    public synchronized List<Stock> read() throws Exception {
        try {
            List<Stock> stocks = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Stock stock = delegate.read();
                if (stock == null) break;
                stocks.add(stock);
            }
            return stocks.isEmpty() ? null : stocks;
        } catch (Exception e) {
            log.error("Error occurred while reading stocks in bulk: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
