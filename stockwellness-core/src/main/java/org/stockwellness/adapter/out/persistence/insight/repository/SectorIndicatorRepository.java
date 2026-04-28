package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.SectorIndicatorJpaEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface SectorIndicatorRepository extends JpaRepository<SectorIndicatorJpaEntity, Long> {
    Optional<SectorIndicatorJpaEntity> findByBaseDateAndSectorCode(LocalDate baseDate, String sectorCode);
}
