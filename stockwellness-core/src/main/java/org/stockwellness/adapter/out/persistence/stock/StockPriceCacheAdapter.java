package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockPriceCacheAdapter {

    private final StockPriceRepository stockPriceRepository;

    public List<StockPriceResult> loadPricesByYear(String ticker, int year) {
        return stockPriceRepository.findAllByTickerAndYear(ticker, year);
    }
}
