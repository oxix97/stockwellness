package org.stockwellness.adapter.out.persistence.stock;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRiskStatusRepository;
import org.stockwellness.application.port.out.stock.StockRiskStatusPort;
import org.stockwellness.domain.stock.StockRiskStatus;

@Component
@RequiredArgsConstructor
public class StockRiskStatusAdapter implements StockRiskStatusPort {
    private final StockRiskStatusRepository repository;

    @Override
    @Transactional
    public void upsert(List<StockRiskStatus> values) {
        if (values == null || values.isEmpty()) return;
        var existing = repository.findAllForUpdate(values.stream().map(StockRiskStatus::getStockId).toList()).stream()
                .collect(Collectors.toMap(StockRiskStatus::getStockId, Function.identity()));
        for (StockRiskStatus value : values) {
            if (existing.containsKey(value.getStockId())) existing.put(value.getStockId(), value);
            else existing.put(value.getStockId(), value);
        }
        repository.saveAllAndFlush(existing.values());
    }
}
