package org.stockwellness.batch.job.sector.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorAiItemWriter implements ItemWriter<SectorInsight> {

    private final SectorInsightPort sectorInsightPort;

    @Override
    public void write(Chunk<? extends SectorInsight> chunk) {
        List<? extends SectorInsight> items = chunk.getItems();
        if (!items.isEmpty()) {
            sectorInsightPort.saveAll(items);
            log.info("AI 분석 의견이 포함된 섹터 인사이트 {}건 저장 완료", items.size());
        }
    }
}
