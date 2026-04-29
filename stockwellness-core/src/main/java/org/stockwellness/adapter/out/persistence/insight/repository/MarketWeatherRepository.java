package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.MarketWeather;

import java.util.Optional;

public interface MarketWeatherRepository extends JpaRepository<MarketWeather, Long> {
    Optional<MarketWeather> findFirstByMarketTypeOrderByBaseDateDesc(String marketType);
}
