package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;
import org.stockwellness.application.port.out.stock.PopularSearchPort;
import org.stockwellness.application.port.out.stock.SearchHistoryPort;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService implements StockSearchUseCase {

    private final SearchHistoryPort searchHistoryPort;
    private final PopularSearchPort popularSearchPort;

    private static final Duration HISTORY_TTL = Duration.ofDays(30);

    @Override
    public void saveSearchHistory(Long memberId, String keyword) {
        searchHistoryPort.save(memberId, keyword);
        searchHistoryPort.setExpireTime(memberId, HISTORY_TTL);
    }

    @Override
    public List<String> getRecentSearches(Long memberId) {
        return searchHistoryPort.findAll(memberId);
    }

    @Override
    public void removeSearchHistory(Long memberId, String keyword) {
        searchHistoryPort.delete(memberId, keyword);
    }

    @Override
    public void clearSearchHistory(Long memberId) {
        searchHistoryPort.deleteAll(memberId);
    }

    @Override
    public List<String> getPopularSearches() {
        return popularSearchPort.findTop10();
    }
}
