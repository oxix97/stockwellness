package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorInsight;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorInsightPort {
    void save(SectorInsight sectorInsight);
    void saveAll(List<SectorInsight> sectorInsights);
    Optional<SectorInsight> findLatestBefore(String sectorCode, LocalDate date);
    
    List<SectorInsight> findTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit);
    List<SectorInsight> findTopSectorsBySupply(LocalDate date, MarketType marketType, int limit);
    Optional<SectorInsight> findBySectorCodeAndDate(String sectorCode, LocalDate date);

    List<BigDecimal> findPastPrices(String sectorCode, LocalDate date, int limit);

    /**
     * 특정 날짜의 시장 전체 지수와 섹터 지수들을 포함한 정보를 리스트로 가져옵니다. (비교용)
     */
    List<SectorInsight> findByCodesAndDate(List<String> codes, LocalDate date);

    /**
     * 특정 기간 동안의 지수 이력을 리스트로 가져옵니다.
     */
    List<SectorInsight> findHistoryByCode(String code, LocalDate endDate, int limit);
}
