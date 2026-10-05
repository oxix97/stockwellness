package org.stockwellness.adapter.out.persistence.stock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.EodStockPriceRepository;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.domain.stock.price.EodStockPrice;

@Component
@RequiredArgsConstructor
public class EodStockPriceAdapter implements EodStockPricePort {
    private final EodStockPriceRepository repository;

    @Override
    @Transactional
    public void upsert(List<EodStockPrice> values) {
        if (values == null || values.isEmpty()) return;
        var existing = repository.findAllForUpdate(values.stream().map(EodStockPrice::getId).toList()).stream()
                .collect(Collectors.toMap(EodStockPrice::getId, Function.identity()));
        Instant now = Instant.now();
        for (EodStockPrice value : values) {
            EodStockPrice current = existing.get(value.getId());
            if (current != null) current.reviseFrom(value, now);
            else existing.put(value.getId(), value);
        }
        repository.saveAllAndFlush(existing.values());
    }
}
