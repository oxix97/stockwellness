package org.stockwellness.adapter.batch.sector.step.writer;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.insight.SectorInsight;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorInsightItemWriter implements ItemWriter<SectorInsight> {

    private final SectorInsightPort sectorInsightPort;

    @Override
    public void write(Chunk<? extends SectorInsight> chunk) {
        List<? extends SectorInsight> items = chunk.getItems();
        if (!items.isEmpty()) {
            sectorInsightPort.saveAll(items);
            log.info("섹터 인사이트 {}건 저장 완료", items.size());
        }
    }
}
