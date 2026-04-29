package org.stockwellness.adapter.out.persistence.insight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.adapter.out.persistence.insight.SectorWeather;

import java.time.LocalDate;
import java.util.List;

public interface SectorWeatherRepository extends JpaRepository<SectorWeather, Long> {
    List<SectorWeather> findAllByBaseDate(LocalDate baseDate);
}
