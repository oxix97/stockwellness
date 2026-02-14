package org.stockwellness.adapter.in.event.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.redis.PopularSearchRedisAdapter;
import org.stockwellness.domain.stock.event.StockSearchEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSearchEventListener {

    private final PopularSearchRedisAdapter popularSearchRedisAdapter;

    @Async
    @EventListener
    public void handleSearchEvent(StockSearchEvent event) {
        log.debug("Handling search event for keyword: {}", event.keyword());
        popularSearchRedisAdapter.incrementCount(event.keyword());
    }
}
