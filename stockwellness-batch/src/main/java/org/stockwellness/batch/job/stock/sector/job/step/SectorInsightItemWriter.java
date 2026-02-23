package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorInsightItemWriter implements ItemWriter<SectorInsight> {

    // [Port] DB에 엔티티를 저장하는 인터페이스 (내부적으로 JPA 사용)
    private final SaveSectorInsightPort saveSectorInsightPort;

    @Override
    public void write(Chunk<? extends SectorInsight> chunk) {
        List<SectorInsight> items = (List<SectorInsight>) chunk.getItems();
        
        log.info("SectorInsight {}건 DB 저장(Bulk Insert) 시작", items.size());
        
        // 헥사고날 포트를 통해 영속성 어댑터로 전달
        saveSectorInsightPort.saveAll(items);
        
        log.info("SectorInsight DB 저장 완료");
    }
}