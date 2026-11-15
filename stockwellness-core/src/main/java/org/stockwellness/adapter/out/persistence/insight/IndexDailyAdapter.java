package org.stockwellness.adapter.out.persistence.insight;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexDailyRepository;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.domain.outbox.OutboxEvent;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

@Component
@RequiredArgsConstructor
public class IndexDailyAdapter implements IndexDailyPort {
    private final IndexDailyRepository repository;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void upsert(List<IndexDaily> values) {
        if (values == null || values.isEmpty()) return;
        var existing = repository.findAllForUpdate(values.stream().map(IndexDaily::getId).toList()).stream()
                .collect(Collectors.toMap(IndexDaily::getId, Function.identity()));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (IndexDaily value : values) {
            IndexDaily current = existing.get(value.getId());
            if (current != null) {
                int previousRevision = current.getRevision();
                boolean wasFinal = current.getPriceStatus() == BenchmarkDataStatus.FINAL;
                current.reviseFrom(value, now);
                if (wasFinal && current.getPriceStatus() == BenchmarkDataStatus.FINAL
                        && current.getRevision() > previousRevision) {
                    saveRevisionEvent(MarketDataRevisionEvent.index(value.getId().getSeriesId(),
                            value.getId().getTradeDate(), current.getRevision()));
                }
            } else {
                existing.put(value.getId(), value);
            }
        }
        repository.saveAllAndFlush(existing.values());
    }

    private void saveRevisionEvent(MarketDataRevisionEvent event) {
        try {
            outboxPort.save(OutboxEvent.create(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize market data revision event", ex);
        }
    }
}
