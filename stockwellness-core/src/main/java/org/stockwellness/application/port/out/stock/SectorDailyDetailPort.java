package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;

import org.stockwellness.domain.stock.insight.SectorDailyDetail;

public interface SectorDailyDetailPort {

    void saveAll(List<? extends SectorDailyDetail> details);

    List<SectorDailyDetail> findByBaseDate(LocalDate baseDate);

    void deleteByBaseDateAndSectorCodeNotIn(LocalDate baseDate, List<String> sectorCodes);
}
