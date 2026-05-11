package org.stockwellness.adapter.out.persistence.stock;

import java.util.List;
import java.util.Optional;

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
import org.stockwellness.domain.stock.exception.StockPriceException;
import static org.stockwellness.global.error.ErrorCode.STOCK_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class StockAdapter implements StockPort {

    private final StockRepository stockRepository;

    @Override
    public Stock findByName(String name) {
        return stockRepository.findByName(name)
                .orElseThrow(() -> new StockPriceException(STOCK_NOT_FOUND));
    }

    @Override
    public List<Stock> findBySectorCode(String sectorCode) {
        if (sectorCode == null) {
            return stockRepository.findAllByActiveStock();
        }
        return stockRepository.findBySector_SectorCodeAndStatus(sectorCode, StockStatus.ACTIVE);
    }

    @Override
    public List<Stock> loadStocksByTickers(List<String> tickers) {
        return stockRepository.findAllByTickerIn(tickers);
    }

    @Override
    public boolean existsByTicker(String ticker) {
        return stockRepository.findByTicker(ticker).isPresent();
    }

    @Override
    public Optional<Stock> loadStockByTicker(String ticker) {
        return stockRepository.findByTicker(ticker);
    }

    @Override
    public Slice<Stock> searchStocks(SearchStockQuery query) {
        PageRequest pageable = PageRequest.of(query.page() - 1, query.size(), Sort.by("name").ascending());
        return stockRepository.searchByCondition(query.keyword(), query.marketType(), query.status(), query.sectorCode(), query.sectorName(), pageable);
    }

    @Override
    public void saveAll(List<Stock> stocks) {
        stockRepository.saveAll(stocks);
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

    @Override
    public List<Stock> findAllByActiveStocks() {
        return stockRepository.findAllByActiveStocks();
    }

    public List<Stock> findByIsinCodeIn(List<String> isinCodes) {
        return stockRepository.findAllByTickerIn(isinCodes);
    }

    public Slice<Stock> searchByCondition(String keyword, MarketType marketType, StockStatus status, String sectorCode, String sectorName, Pageable pageable) {
        return stockRepository.searchByCondition(keyword, marketType, status, sectorCode, sectorName, pageable);
    }

    public List<String> findAllStockCodes() {
        return stockRepository.findAllByTicker();
    }
}
