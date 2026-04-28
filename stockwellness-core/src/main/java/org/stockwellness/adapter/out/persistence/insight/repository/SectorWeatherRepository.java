package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.SectorWeather;

public interface SectorWeatherRepository extends JpaRepository<SectorWeather, Long> {
}
