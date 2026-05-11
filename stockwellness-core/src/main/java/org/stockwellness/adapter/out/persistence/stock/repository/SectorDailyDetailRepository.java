package org.stockwellness.adapter.out.persistence.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;

public interface SectorDailyDetailRepository extends JpaRepository<SectorDailyDetail, Long> {

    Optional<SectorDailyDetail> findBySectorCodeAndBaseDate(String sectorCode, LocalDate baseDate);

    List<SectorDailyDetail> findBySectorCodeInAndBaseDate(List<String> sectorCodes, LocalDate baseDate);

    List<SectorDailyDetail> findByBaseDateOrderBySectorCodeAsc(LocalDate baseDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SectorDailyDetail s where s.baseDate = :baseDate and s.sectorCode not in :sectorCodes")
    void deleteByBaseDateAndSectorCodeNotIn(@Param("baseDate") LocalDate baseDate, @Param("sectorCodes") List<String> sectorCodes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SectorDailyDetail s where s.baseDate = :baseDate")
    void deleteByBaseDate(@Param("baseDate") LocalDate baseDate);
}
