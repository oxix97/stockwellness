package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorInsightItemWriter implements ItemWriter<List<SectorInsight>> {

    private final SectorInsightPort sectorInsightPort;

    @Override
    public void write(Chunk<? extends List<SectorInsight>> chunk) {
        List<SectorInsight> allInsights = new ArrayList<>();
        for (List<SectorInsight> insights : chunk) {
            if (insights != null) {
                allInsights.addAll(insights);
            }
        }

        if (!allInsights.isEmpty()) {
            sectorInsightPort.saveAll(allInsights);
            log.info("Successfully saved {} sector insights.", allInsights.size());
        }
    }
}
