package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockInvestorTradeReadPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

class StockPriceAdapterCanonicalReadTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 12);

    private final StockPriceRepository legacyStockWrites = mock(StockPriceRepository.class);
    private final BenchmarkPriceRepository legacyBenchmarkWrites = mock(BenchmarkPriceRepository.class);
    private final StockPriceReadPort canonicalPrices = mock(StockPriceReadPort.class);
    private final StockInvestorTradeReadPort investorTrades = mock(StockInvestorTradeReadPort.class);
    private final StockPort stocks = mock(StockPort.class);
    private final IndexDailyReadPort indexes = mock(IndexDailyReadPort.class);
    private final StockPriceAdapter adapter = new StockPriceAdapter(
            legacyStockWrites, legacyBenchmarkWrites, canonicalPrices, investorTrades, stocks, indexes);

    @Test
    void legacy_write_methods_and_benchmark_repository_methods_keep_their_ports() {
        StockPrice stockPrice = mock(StockPrice.class);
        BenchmarkPrice benchmark = mock(BenchmarkPrice.class);
        when(legacyStockWrites.save(stockPrice)).thenReturn(stockPrice);
        when(legacyBenchmarkWrites.findByTickerAndBaseDate("^KS11", DATE)).thenReturn(Optional.of(benchmark));
        when(legacyBenchmarkWrites.findTopByTickerAndBaseDateLessThanOrderByBaseDateDesc("^KS11", DATE))
                .thenReturn(Optional.of(benchmark));
        when(legacyBenchmarkWrites.findByTickerAndBaseDateLessThanEqualOrderByBaseDateDesc(
                eq("^KS11"), eq(DATE), any())).thenReturn(List.of(benchmark));

        assertThat(adapter.save(stockPrice)).isSameAs(stockPrice);
        adapter.saveAll(List.of(stockPrice));
        adapter.save(benchmark);
        assertThat(adapter.findByTickerAndBaseDate("^KS11", DATE)).contains(benchmark);
        assertThat(adapter.findLatestBefore("^KS11", DATE)).contains(benchmark);
        assertThat(adapter.findHistoryByTicker("^KS11", DATE, 5)).containsExactly(benchmark);

        verify(legacyStockWrites).saveAll(List.of(stockPrice));
        verify(legacyBenchmarkWrites).save(benchmark);
    }

    @Test
    void investor_rankings_use_final_normalized_source() {
        StockSupplyRankingResult result = new StockSupplyRankingResult(
                "005930", "삼성전자", "반도체", BigDecimal.TEN, BigDecimal.ONE, 10L, BigDecimal.TEN, BigDecimal.TEN);
        when(investorTrades.topStocks(DATE, "KRX", "INSTITUTION_TOTAL", TradeDirection.BUY, 10))
                .thenReturn(List.of(result));
        when(investorTrades.topStocks(DATE, "KRX", "FOREIGN", TradeDirection.SELL, 5))
                .thenReturn(List.of(result));
        when(investorTrades.latestFinalDate("KRX")).thenReturn(Optional.of(DATE));

        assertThat(adapter.findTopInstitutionStocksBySupply(DATE, TradeDirection.BUY, 10)).containsExactly(result);
        assertThat(adapter.findTopForeignStocksBySupply(DATE, TradeDirection.SELL, 5)).containsExactly(result);
        assertThat(adapter.findLatestInvestorTradeDate()).contains(DATE);
    }

    @Test
    void batch_history_uses_only_korean_final_prices_and_keeps_ascending_order() {
        Stock samsung = stock(1L, "005930", MarketType.KOSPI);
        Stock apple = stock(2L, "AAPL", MarketType.NASDAQ);
        PriceObservation newest = observation(1L, DATE.minusDays(1), "110", "100");
        PriceObservation oldest = observation(1L, DATE.minusDays(2), "100", "90");
        when(canonicalPrices.recent(Set.of(1L), "KRX", DATE.minusDays(1), 2, PriceBasis.RAW))
                .thenReturn(Map.of(1L, List.of(newest, oldest)));

        Map<Long, List<StockPrice>> result = adapter.findRecentPricesWithDateByStocks(
                List.of(samsung, apple), DATE, 2);

        assertThat(result.get(1L)).extracting(StockPrice::getClosePrice)
                .containsExactly(new BigDecimal("100"), new BigDecimal("110"));
        assertThat(result).doesNotContainKey(2L);
        verify(canonicalPrices).recent(Set.of(1L), "KRX", DATE.minusDays(1), 2, PriceBasis.RAW);
    }

    @Test
    void unsupported_overseas_stocks_are_not_queried_as_krx() {
        Stock apple = stock(2L, "AAPL", MarketType.NASDAQ);
        when(stocks.loadStockByTicker("AAPL")).thenReturn(Optional.of(apple));
        when(stocks.loadStocksByTickers(List.of("AAPL"))).thenReturn(List.of(apple));

        assertThat(adapter.findRecentPricesWithDateByStock(apple, DATE, 2)).isEmpty();
        assertThat(adapter.findLatestByTicker("AAPL")).isEmpty();
        assertThat(adapter.findAllLatestByTickers(List.of("AAPL"))).isEmpty();
        assertThat(adapter.loadRecentHistoriesBatch(List.of("AAPL"), 2)).isEmpty();
        assertThat(adapter.loadPricesByTicker("AAPL", DATE.minusDays(5), DATE)).isEmpty();
        assertThat(adapter.loadPricesByTickers(List.of("AAPL"), DATE.minusDays(5), DATE)).isEmpty();
        assertThat(adapter.findRecentPricesWithDateByStocks(List.of(apple), DATE, 2)).isEmpty();
        assertThat(adapter.findLatestBaseDatesByStocks(List.of(apple))).isEmpty();
        verifyNoInteractions(canonicalPrices);
    }

    @Test
    void current_and_historical_price_reads_map_canonical_final_values() {
        Stock samsung = stock(1L, "005930", MarketType.KOSPI);
        PriceObservation finalRow = observation(1L, DATE, "110", "100");
        when(stocks.loadStockByTicker("005930")).thenReturn(Optional.of(samsung));
        when(stocks.loadStocksByTickers(List.of("005930"))).thenReturn(List.of(samsung));
        when(canonicalPrices.latest(eq(Set.of(1L)), eq("KRX"), any(LocalDate.class), eq(PriceBasis.RAW)))
                .thenReturn(Map.of(1L, finalRow));
        when(canonicalPrices.recent(eq(Set.of(1L)), eq("KRX"), any(LocalDate.class), eq(1), eq(PriceBasis.RAW)))
                .thenReturn(Map.of(1L, List.of(finalRow)));
        when(canonicalPrices.history(Set.of(1L), "KRX", DATE.minusDays(1), DATE, PriceBasis.RAW))
                .thenReturn(Map.of(1L, List.of(finalRow)));

        assertThat(adapter.findRecentPricesWithDateByStock(samsung, DATE, 1).getFirst().getFluctuationRate())
                .isEqualByComparingTo("10.0");
        assertThat(adapter.findLatestByTicker("005930").orElseThrow().getClosePrice())
                .isEqualByComparingTo("110");
        assertThat(adapter.findAllLatestByTickers(List.of("005930"))).containsEntry("005930", new BigDecimal("110"));
        assertThat(adapter.loadRecentHistoriesBatch(List.of("005930"), 1).get("005930"))
                .hasSize(1);
        assertThat(adapter.loadPricesByTicker("005930", DATE.minusDays(1), DATE).getFirst().changeRate())
                .isEqualByComparingTo("10.0");
        assertThat(adapter.loadPricesByTickers(List.of("005930"), DATE.minusDays(1), DATE)
                .get("005930").getFirst().closePrice()).isEqualByComparingTo("110");
    }

    @Test
    void stock_date_helpers_and_market_reads_use_only_active_korean_stocks() {
        Stock samsung = stock(1L, "005930", MarketType.KOSPI);
        Stock apple = stock(2L, "AAPL", MarketType.NASDAQ);
        PriceObservation finalRow = observation(1L, DATE, "110", "100");
        when(stocks.findAllByActiveStocks()).thenReturn(List.of(samsung, apple));
        when(canonicalPrices.latestFinalDate("KRX", DATE)).thenReturn(Optional.of(DATE));
        when(canonicalPrices.latest(eq(Set.of(1L)), eq("KRX"), any(LocalDate.class), eq(PriceBasis.RAW)))
                .thenReturn(Map.of(1L, finalRow));
        when(canonicalPrices.history(Set.of(1L), "KRX", DATE, DATE, PriceBasis.RAW))
                .thenReturn(Map.of(1L, List.of(finalRow)));

        assertThat(adapter.findLatestDateOnOrBefore(DATE)).contains(DATE);
        assertThat(adapter.existsByBaseDate(DATE)).isTrue();
        assertThat(adapter.existsByBaseDate(DATE.plusDays(1))).isFalse();
        assertThat(adapter.findLatestBaseDatesByStocks(List.of(samsung, apple))).containsEntry(1L, DATE);
        assertThat(adapter.findAllByDate(DATE)).hasSize(1);
        assertThat(adapter.findAllBreadthItemsByDate(DATE)).hasSize(1);
        assertThat(adapter.findLateststockPrice("ignored")).isEmpty();

        verify(canonicalPrices, times(2)).history(Set.of(1L), "KRX", DATE, DATE, PriceBasis.RAW);
    }

    @Test
    void breadth_summary_uses_prior_final_close_and_ignores_rows_without_it() {
        List<Stock> universe = List.of(
                stock(1L, "000001", MarketType.KOSPI), stock(2L, "000002", MarketType.KOSDAQ),
                stock(3L, "000003", MarketType.KOSPI), stock(4L, "000004", MarketType.KOSDAQ),
                stock(5L, "000005", MarketType.KOSPI), stock(6L, "AAPL", MarketType.NASDAQ));
        when(stocks.findAllByActiveStocks()).thenReturn(universe);
        when(canonicalPrices.history(eq(Set.of(1L, 2L, 3L, 4L, 5L)), eq("KRX"), eq(DATE), eq(DATE),
                eq(PriceBasis.RAW))).thenReturn(Map.of(
                        1L, List.of(breadthObservation(1L, "101", "100", "101", "100")),
                        2L, List.of(breadthObservation(2L, "99.8", "100", "100", "99.8")),
                        3L, List.of(breadthObservation(3L, "100", "100", "100.5", "99.5")),
                        4L, List.of(breadthObservation(4L, "103", "100", "104", "100")),
                        5L, List.of(breadthObservation(5L, "101", null, "101", "100"))));

        var summary = adapter.summarizeBreadthByDate(DATE);

        assertThat(summary.total()).isEqualTo(4);
        assertThat(summary.advancing()).isEqualTo(2);
        assertThat(summary.declining()).isEqualTo(1);
        assertThat(summary.unchanged()).isEqualTo(1);
        assertThat(summary.highVolatility()).isEqualTo(1);
        assertThat(summary.advanceRatio()).isEqualByComparingTo("0.5000");
    }

    @Test
    void no_breadth_data_returns_null() {
        when(stocks.findAllByActiveStocks()).thenReturn(List.of());

        assertThat(adapter.summarizeBreadthByDate(DATE)).isNull();
    }

    private static Stock stock(long id, String ticker, MarketType marketType) {
        Stock stock = mock(Stock.class);
        when(stock.getId()).thenReturn(id);
        when(stock.getTicker()).thenReturn(ticker);
        when(stock.getMarketType()).thenReturn(marketType);
        return stock;
    }

    private static PriceObservation observation(long id, LocalDate date, String close, String previous) {
        return new PriceObservation(id, date, "KRX", PriceBasis.RAW, new BigDecimal(close),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), 100L,
                new BigDecimal("10000"), "FINAL", 1, new BigDecimal(close), new BigDecimal(previous),
                new BigDecimal("105"), new BigDecimal("102"), new BigDecimal("99"), new BigDecimal("98"),
                new BigDecimal("55"), BigDecimal.ONE, new BigDecimal("0.5"), new BigDecimal("20"),
                new BigDecimal("25"), new BigDecimal("15"));
    }

    private static PriceObservation breadthObservation(long id, String close, String previous, String high, String low) {
        return new PriceObservation(id, DATE, "KRX", PriceBasis.RAW, new BigDecimal(close), new BigDecimal(high),
                new BigDecimal(low), new BigDecimal(close), 100L, new BigDecimal("10000"), "FINAL", 1,
                null, previous == null ? null : new BigDecimal(previous), null, null, null, null, null, null,
                null, null, null, null);
    }
}
