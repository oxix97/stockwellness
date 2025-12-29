package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockJpaRepository;
import org.stockwellness.application.port.out.StockRepository;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockAdapter implements StockRepository {

    private final StockJpaRepository stockRepository;

    @Override
    public Stock save(Stock stock) {
        return stockRepository.save(stock);
    }

    @Override
    public Optional<Stock> findByTicker(String ticker) {
        return stockRepository.findByTicker(ticker);
    }

    @Override
    public List<Stock> findByMarketTypeAndStatus(MarketType marketType, StockStatus status) {
        return stockRepository.findByMarketTypeAndStatus(marketType, status);
    }

    @Override
    public List<Stock> findByStatus(StockStatus status) {
        return stockRepository.findByStatus(status);
    }

    @Override
    public List<Stock> findByIsinCodeIn(List<String> isinCodes) {
        return stockRepository.findByIsinCodeIn(isinCodes);
    }

    @Override
    public Slice<Stock> searchByCondition(String keyword, MarketType marketType, StockStatus status, Pageable pageable) {
        return stockRepository.searchByCondition(keyword, marketType, status, pageable);
    }
}
