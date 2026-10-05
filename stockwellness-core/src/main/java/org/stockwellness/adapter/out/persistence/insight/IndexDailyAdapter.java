package org.stockwellness.adapter.out.persistence.insight;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexDailyRepository;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.domain.stock.insight.IndexDaily;

@Component
@RequiredArgsConstructor
public class IndexDailyAdapter implements IndexDailyPort {
    private final IndexDailyRepository repository;

    @Override
    @Transactional
    public void upsert(List<IndexDaily> values) {
        if (values == null || values.isEmpty()) return;
        var existing = repository.findAllForUpdate(values.stream().map(IndexDaily::getId).toList()).stream()
                .collect(Collectors.toMap(IndexDaily::getId, Function.identity()));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (IndexDaily value : values) {
            IndexDaily current = existing.get(value.getId());
            if (current != null) current.reviseFrom(value, now);
            else existing.put(value.getId(), value);
        }
        repository.saveAllAndFlush(existing.values());
    }
}
