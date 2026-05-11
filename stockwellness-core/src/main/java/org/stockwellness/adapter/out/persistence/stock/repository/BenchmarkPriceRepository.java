package org.stockwellness.adapter.out.persistence.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

public interface BenchmarkPriceRepository extends JpaRepository<BenchmarkPrice, Long> {
    Optional<BenchmarkPrice> findByTickerAndBaseDate(String ticker, LocalDate baseDate);

    /**
     * 특정 지수의 특정 일자 이전 가장 최근 시세를 조회합니다.
     */
    Optional<BenchmarkPrice> findTopByTickerAndBaseDateLessThanOrderByBaseDateDesc(String ticker, LocalDate baseDate);

    List<BenchmarkPrice> findByTickerAndBaseDateLessThanEqualOrderByBaseDateDesc(String ticker, LocalDate baseDate, Pageable pageable);
}
