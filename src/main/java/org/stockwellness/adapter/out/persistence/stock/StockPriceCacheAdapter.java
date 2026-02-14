package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockPriceCacheAdapter {

    private final StockPriceRepository stockPriceRepository;

    /**
     * 연도별 시세 데이터를 캐싱합니다.
     * 캐시 키 예시: stock_prices::AAPL:2024
     */
    @Cacheable(value = "stock_prices", key = "#ticker + ':' + #year")
    public List<StockPriceResult> loadPricesByYear(String ticker, int year) {
        return stockPriceRepository.findAllByTickerAndYear(ticker, year);
    }
}
