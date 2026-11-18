package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

@ExtendWith(MockitoExtension.class)
class CanonicalBenchmarkPriceAdapterTest {
    @InjectMocks private CanonicalBenchmarkPriceAdapter adapter;
    @Mock private JdbcTemplate jdbc;
    @Mock private IndexSeriesRepository seriesRepository;
    @Mock private IndexDailyPort indexDailyPort;

    @Test
    void savesBenchmarkPricesToTheCanonicalSeriesAsProvisional() {
        MarketIndex index = MarketIndex.ofOverseasBenchmark("US:SPX", "S&P 500", "SPX");
        IndexSeries series = new IndexSeries(index, "KIS", "kis.ovrs.idxcode.N", "SPX", "NYSE",
                "PRICE", "NONE", "USD", true);
        given(seriesRepository.findCanonical("SPX", "PRICE", "NONE", "USD")).willReturn(List.of(series));
        BenchmarkPrice price = BenchmarkPrice.of("S&P 500", "SPX", LocalDate.of(2026, 11, 13), new BigDecimal("6500"));
        price.updatePrices(new BigDecimal("6400"), new BigDecimal("6520"), new BigDecimal("6390"),
                new BigDecimal("6500"), null, 100L);

        adapter.save(price);

        ArgumentCaptor<List<IndexDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexDailyPort).upsert(captor.capture());
        IndexDaily saved = captor.getValue().getFirst();
        assertThat(saved.getSeries()).isSameAs(series);
        assertThat(saved.getClosePrice()).isEqualByComparingTo("6500");
        assertThat(saved.getPriceStatus()).isEqualTo(BenchmarkDataStatus.PROVISIONAL);
    }

    @Test
    void rejectsIndexesOutsideTheCanonicalCoverage() {
        BenchmarkPrice unsupported = BenchmarkPrice.of("NASDAQ Composite", "COMP",
                LocalDate.of(2026, 11, 13), BigDecimal.ONE);

        assertThatThrownBy(() -> adapter.save(unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported canonical benchmark");
        verifyNoInteractions(seriesRepository, indexDailyPort);
    }
}
