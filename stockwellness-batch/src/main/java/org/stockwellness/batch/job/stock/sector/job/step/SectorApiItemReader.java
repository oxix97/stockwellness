package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SectorApiItemReader implements ItemReader<SectorApiDto> {

    private final SectorDataPort sectorDataPort;

    private Iterator<SectorApiDto> sectorDataIterator;

    @Override
    public SectorApiDto read() {
        if (sectorDataIterator == null) {
            log.info("업종별 시세 데이터 호출 시작...");
            List<SectorApiDto> apiData = sectorDataPort.fetchTodaySectorData();
            
            if (apiData == null || apiData.isEmpty()) {
                throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
            }
            
            sectorDataIterator = apiData.iterator();
            log.info("총 {}개의 업종 데이터 로드 완료.", apiData.size());
        }

        if (sectorDataIterator.hasNext()) {
            return sectorDataIterator.next();
        } else {
            return null;
        }
    }
}
