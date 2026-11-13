package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;

class StockPriceAdapterCanonicalBenchmarkTest {
    private final StockPriceRepository stockPriceRepository = mock(StockPriceRepository.class);
    private final BenchmarkPriceRepository benchmarkPriceRepository = mock(BenchmarkPriceRepository.class);
    private final StockPriceCacheAdapter stockPriceCacheAdapter = mock(StockPriceCacheAdapter.class);
    private final IndexDailyReadPort indexDailyReadPort = mock(IndexDailyReadPort.class);
    private final StockPriceAdapter adapter = new StockPriceAdapter(stockPriceRepository, benchmarkPriceRepository,
            stockPriceCacheAdapter, indexDailyReadPort);

    @Test
    void loadBenchmarkPrices_reads_final_index_daily_and_calculates_change_from_previous_final_close() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(indexDailyReadPort.historyFinal("0001", date, date)).thenReturn(List.of(
                new IndexPriceObservation(10L, date, new BigDecimal("105"), new BigDecimal("95"),
                        new BigDecimal("105"), null, null, null, null, null, 1, "FINAL",
                        new BigDecimal("100"), 1100L, new BigDecimal("115000"), new BigDecimal("100"))));

        var prices = adapter.loadBenchmarkPrices("^KS11", date, date);

        assertThat(prices).hasSize(1);
        assertThat(prices.getFirst().ticker()).isEqualTo("^KS11");
        assertThat(prices.getFirst().changeRate()).isEqualByComparingTo("5.00000000");
        assertThat(prices.getFirst().closePrice()).isEqualByComparingTo("105");
        assertThat(prices.getFirst().transactionAmt()).isEqualByComparingTo("115000");
        verify(indexDailyReadPort).historyFinal("0001", date, date);
    }

    @Test
    void unsupported_kis_benchmark_does_not_fall_back_to_legacy_benchmark_table() {
        var prices = adapter.loadBenchmarkPrices("SOX", LocalDate.now(), LocalDate.now());

        assertThat(prices).isEmpty();
        verifyNoInteractions(indexDailyReadPort);
    }
}
