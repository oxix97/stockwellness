package org.stockwellness.batch.job.stock.sector.job.step;

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
public class SectorInsightItemWriter implements ItemWriter<SectorInsight> {

    // [Port] DB에 엔티티를 저장하는 인터페이스 (헥사고날 포트 활용)
    private final SectorInsightPort sectorInsightPort;

    @Override
    public void write(Chunk<? extends SectorInsight> chunk) {
        List<SectorInsight> items = (List<SectorInsight>) chunk.getItems();
        
        log.info("SectorInsight {}건 DB 저장 시작", items.size());
        
        // 통합된 포트를 통해 영속성 어댑터로 전달
        sectorInsightPort.saveAll(items);
        
        log.info("SectorInsight DB 저장 완료");
    }
}
