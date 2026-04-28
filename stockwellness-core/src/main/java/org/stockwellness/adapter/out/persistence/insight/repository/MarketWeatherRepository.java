package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.MarketWeatherJpaEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface MarketWeatherRepository extends JpaRepository<MarketWeatherJpaEntity, Long> {
    Optional<MarketWeatherJpaEntity> findByBaseDateAndMarketType(LocalDate baseDate, String marketType);
    Optional<MarketWeatherJpaEntity> findFirstByMarketTypeOrderByBaseDateDesc(String marketType);
}
