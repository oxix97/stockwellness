package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.SectorWeatherJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorWeatherRepository extends JpaRepository<SectorWeatherJpaEntity, Long> {
    Optional<SectorWeatherJpaEntity> findByBaseDateAndSectorCode(LocalDate baseDate, String sectorCode);
    List<SectorWeatherJpaEntity> findByBaseDateOrderByWeatherScoreDesc(LocalDate baseDate);
}
