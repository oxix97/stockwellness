package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.time.LocalDate;
import java.util.Optional;

public interface BenchmarkPriceRepository extends JpaRepository<BenchmarkPrice, Long> {
    Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate);
}
