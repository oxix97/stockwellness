package org.stockwellness.adapter.batch.sector.step.writer;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorDailyDetailPort;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorDailyDetailItemWriter implements ItemWriter<SectorDailyDetail> {

    private final SectorDailyDetailPort sectorDailyDetailPort;

    @Override
    public void write(Chunk<? extends SectorDailyDetail> chunk) {
        List<? extends SectorDailyDetail> items = chunk.getItems();
        if (!items.isEmpty()) {
            sectorDailyDetailPort.saveAll(items);
            log.info("섹터 일별 상세 {}건 저장 완료", items.size());
        }
    }
}
