package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorInsightRepository extends JpaRepository<SectorInsight, Long> {
    Optional<SectorInsight> findBySectorCodeAndBaseDate(String sectorCode, LocalDate baseDate);

    Optional<SectorInsight> findFirstBySectorCodeAndBaseDateBeforeOrderByBaseDateDesc(String sectorCode, LocalDate baseDate);

    @Query("SELECT s FROM SectorInsight s WHERE s.baseDate = :date ORDER BY s.avgFluctuationRate DESC")
    List<SectorInsight> findTopByDate(LocalDate date, Pageable pageable);

    @Query("SELECT s FROM SectorInsight s WHERE s.baseDate = :date ORDER BY (s.netForeignBuyAmount + s.netInstBuyAmount) DESC")
    List<SectorInsight> findTopBySupply(LocalDate date, Pageable pageable);

    List<SectorInsight> findAllByBaseDate(LocalDate date);

    @Query("SELECT s.sectorIndexCurrentPrice FROM SectorInsight s WHERE s.sectorCode = :sectorCode AND s.baseDate < :date ORDER BY s.baseDate DESC")
    List<BigDecimal> findPastPrices(String sectorCode, LocalDate date, Pageable pageable);
}
