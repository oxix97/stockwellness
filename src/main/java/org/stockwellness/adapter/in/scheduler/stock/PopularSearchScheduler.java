package org.stockwellness.adapter.in.scheduler.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularSearchScheduler {

    private final StockSearchUseCase stockSearchUseCase;

    /**
     * 매 시간 정각에 인기 검색어 순위 로그 기록 (또는 필요 시 추가 집계 로직 수행)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logPopularSearches() {
        List<String> top10 = stockSearchUseCase.getPopularSearches();
        log.info("Hourly Popular Search Ranking: {}", top10);
    }
}
