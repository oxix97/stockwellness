package org.stockwellness.adapter.in.event.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.shared.event.BatchCompletedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCacheSyncEventListener {

    @EventListener
    @CacheEvict(value = "stock_info", allEntries = true)
    public void handleBatchCompletedEvent(BatchCompletedEvent event) {
        log.info("Batch job {} completed. Invalidating stock_info cache.", event.getJobName());
    }
}