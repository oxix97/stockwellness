package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockAdapter implements StockPort {

    private final StockRepository stockRepository;

    @Override
    public List<Stock> findBySectorMediumCode(String mediumCode) {
        if (mediumCode == null) {
            return stockRepository.findByStatus(StockStatus.ACTIVE);
        }
        return stockRepository.findBySector_MediumCodeAndStatus(mediumCode, StockStatus.ACTIVE);
    }

    @Override
    public List<Stock> loadStocksByTickers(List<String> isinCodes) {
        return stockRepository.findAllById(isinCodes);
    }

    @Override
    public boolean existsByTicker(String isinCode) {
        return stockRepository.existsById(isinCode);
    }

    @Override
    public Optional<Stock> loadStockByTicker(String ticker) {
        return stockRepository.findByTicker(ticker);
    }

    @Override
    public Slice<Stock> searchStocks(SearchStockQuery query) {
        PageRequest pageable = PageRequest.of(query.page() - 1, query.size(), Sort.by("name").ascending());
        return stockRepository.searchByCondition(query.keyword(), query.marketType(), query.status(), pageable);
    }

    public Stock save(Stock stock) {
        return stockRepository.save(stock);
    }


    public Optional<Stock> findByTicker(String ticker) {
        return stockRepository.findByTicker(ticker);
    }


    public List<Stock> findByMarketTypeAndStatus(MarketType marketType, StockStatus status) {
        return stockRepository.findByMarketTypeAndStatus(marketType, status);
    }


    public List<Stock> findByStatus(StockStatus status) {
        return stockRepository.findByStatus(status);
    }


    public List<Stock> findByIsinCodeIn(List<String> isinCodes) {
        return stockRepository.findByTickerIn(isinCodes);
    }


    public Slice<Stock> searchByCondition(String keyword, MarketType marketType, StockStatus status, Pageable pageable) {
        return stockRepository.searchByCondition(keyword, marketType, status, pageable);
    }

    public List<String> findAllStockCodes() {
        return stockRepository.findAllByTicker();
    }
}
