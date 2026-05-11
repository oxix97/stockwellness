package org.stockwellness.adapter.out.persistence.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class, BenchmarkRepositoryImpl.class})
@ActiveProfiles("test")
@DisplayName("BenchmarkRepositoryImpl 통합 테스트")
class BenchmarkRepositoryImplTest {

    @Autowired
    private BenchmarkPriceRepository benchmarkPriceRepository;

    @Autowired
    private BenchmarkRepository benchmarkRepository;

    @Test
    @DisplayName("지수 티커와 날짜 범위로 benchmark_price를 조회한다")
    void findBenchmarkPrices_filtersByTickerAndDateRange() {
        BenchmarkPrice kospiOld = createBenchmarkPrice("코스피 종합", "0001", LocalDate.of(2026, 4, 2), "2490.00", "0.50");
        BenchmarkPrice kospiNew = createBenchmarkPrice("코스피 종합", "0001", LocalDate.of(2026, 4, 3), "2525.00", "1.41");
        BenchmarkPrice kosdaq = createBenchmarkPrice("코스닥 종합", "1001", LocalDate.of(2026, 4, 3), "900.00", "0.10");

        benchmarkPriceRepository.saveAll(List.of(kospiOld, kospiNew, kosdaq));
        benchmarkPriceRepository.flush();

        List<StockPriceResult> results = benchmarkRepository.findBenchmarkPrices(
                "0001",
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3)
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).baseDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(results.get(1).baseDate()).isEqualTo(LocalDate.of(2026, 4, 3));
        assertThat(results.get(0).closePrice()).isEqualByComparingTo("2490.00");
        assertThat(results.get(1).closePrice()).isEqualByComparingTo("2525.00");
        assertThat(results.get(0).changeRate()).isEqualByComparingTo("0.50");
        assertThat(results.get(1).changeRate()).isEqualByComparingTo("1.41");
    }

    private BenchmarkPrice createBenchmarkPrice(String name, String ticker, LocalDate baseDate, String closePrice, String changeRate) {
        BenchmarkPrice benchmarkPrice = BenchmarkPrice.of(name, ticker, baseDate, new BigDecimal(closePrice));
        benchmarkPrice.updatePrices(
                new BigDecimal(closePrice),
                new BigDecimal(closePrice),
                new BigDecimal(closePrice),
                new BigDecimal(closePrice),
                new BigDecimal(changeRate),
                1000L
        );
        return benchmarkPrice;
    }
}
