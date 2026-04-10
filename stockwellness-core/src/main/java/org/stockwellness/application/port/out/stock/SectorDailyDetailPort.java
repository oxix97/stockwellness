package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.insight.SectorDailyDetail;

import java.time.LocalDate;
import java.util.List;

public interface SectorDailyDetailPort {

    void saveAll(List<? extends SectorDailyDetail> details);

    List<SectorDailyDetail> findByBaseDate(LocalDate baseDate);

    void deleteByBaseDateAndSectorCodeNotIn(LocalDate baseDate, List<String> sectorCodes);
}
