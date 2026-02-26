package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.insight.SectorInsight;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorInsightPort {
    void save(SectorInsight sectorInsight);
    void saveAll(List<SectorInsight> sectorInsights);
    Optional<SectorInsight> findLatestBefore(String sectorCode, LocalDate date);
    
    List<SectorInsight> findTopSectorsByFluctuation(LocalDate date, int limit);
    List<SectorInsight> findTopSectorsBySupply(LocalDate date, int limit);
    Optional<SectorInsight> findBySectorCodeAndDate(String sectorCode, LocalDate date);

    List<BigDecimal> findPastPrices(String sectorCode, LocalDate date, int limit);
}
