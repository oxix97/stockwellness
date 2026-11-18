package org.stockwellness.application.service.marketdata;

import java.util.Objects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.batch.MarketDataRevisionUseCase;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@Service
@RequiredArgsConstructor
public class MarketDataRevisionService implements MarketDataRevisionUseCase {
    private static final int MAX_AFFECTED_SESSIONS = 252;

    private final StockPriceReadPort stockPriceReadPort;
    private final IndexDailyReadPort indexDailyReadPort;
    private final SectorLeadingStockPort sectorLeadingStockPort;
    private final MarketDerivedDataUseCase derivedDataUseCase;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recalculate(MarketDataRevisionEvent event) {
        Objects.requireNonNull(event, "Market data revision event is required");
        switch (event.sourceType()) {
            case STOCK_PRICE -> recalculateStock(event);
            case INDEX_PRICE -> recalculateIndex(event);
        }
        saveCompletionEvent(event);
    }

    private void saveCompletionEvent(MarketDataRevisionEvent event) {
        try {
            outboxPort.save(OutboxEvent.create(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize market data revision completion event", ex);
        }
    }

    private void recalculateStock(MarketDataRevisionEvent event) {
        stockPriceReadPort.finalDatesFrom(event.sourceId(), event.quoteScope(), event.tradeDate(), MAX_AFFECTED_SESSIONS)
                .forEach(targetDate -> derivedDataUseCase.calculateStockIndicators(
                        event.sourceId(), event.quoteScope(), targetDate));
        sectorLeadingStockPort.seriesIdsForStock(event.sourceId())
                .forEach(seriesId -> derivedDataUseCase.calculateSectorLeadingStocks(seriesId, event.tradeDate()));
    }

    private void recalculateIndex(MarketDataRevisionEvent event) {
        indexDailyReadPort.finalDatesFrom(event.sourceId(), event.tradeDate(), MAX_AFFECTED_SESSIONS)
                .forEach(targetDate -> {
                    derivedDataUseCase.calculateIndexIndicators(event.sourceId(), targetDate);
                    derivedDataUseCase.calculateIndexWeather(event.sourceId(), targetDate);
                });
        derivedDataUseCase.calculateSectorLeadingStocks(event.sourceId(), event.tradeDate());
    }
}
