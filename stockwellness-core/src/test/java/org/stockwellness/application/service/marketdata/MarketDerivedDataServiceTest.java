package org.stockwellness.application.service.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.application.port.out.stock.IndexWeatherPort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockCandidate;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.application.port.out.stock.TechnicalIndicatorPort;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.domain.stock.price.StockTechnicalIndicator;
import org.stockwellness.domain.stock.insight.IndexTechnicalIndicator;
import org.stockwellness.domain.stock.insight.IndexWeather;

@ExtendWith(MockitoExtension.class)
class MarketDerivedDataServiceTest {
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 5);

    @Mock private StockPriceReadPort stockPrices;
    @Mock private IndexDailyReadPort indexPrices;
    @Mock private IndexWeatherPort indexWeather;
    @Mock private TechnicalIndicatorPort indicators;
    @Mock private SectorLeadingStockPort leadingStocks;
    private MarketDerivedDataService service;

    @BeforeEach
    void setUp() {
        service = new MarketDerivedDataService(stockPrices, indexPrices, indexWeather, indicators, leadingStocks,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void stock_indicators_include_the_target_final_day_and_capture_source_revisions() {
        List<PriceObservation> observations = stockObservations(120, TARGET_DATE);
        given(stockPrices.recent(Set.of(7L), "KRX", TARGET_DATE, 252, PriceBasis.RAW))
                .willReturn(Map.of(7L, observations));

        service.calculateStockIndicators(7L, "KRX", TARGET_DATE);

        ArgumentCaptor<StockTechnicalIndicator> saved = ArgumentCaptor.forClass(StockTechnicalIndicator.class);
        verify(indicators).upsertStock(saved.capture());
        assertThat(saved.getValue().tradeDate()).isEqualTo(TARGET_DATE);
        assertThat(saved.getValue().ma120()).isEqualByComparingTo("159.5000");
        assertThat(saved.getValue().ma20Disparity()).isEqualByComparingTo("1.045346");
        assertThat(saved.getValue().inputManifest()).contains(TARGET_DATE.toString(), "\"revision\":1");
    }

    @Test
    void stock_indicators_are_not_saved_when_target_day_is_missing_or_history_is_short() {
        given(stockPrices.recent(Set.of(7L), "KRX", TARGET_DATE, 252, PriceBasis.RAW))
                .willReturn(Map.of(7L, stockObservations(119, TARGET_DATE.minusDays(1))));

        service.calculateStockIndicators(7L, "KRX", TARGET_DATE);

        verify(indicators, never()).upsertStock(any());
    }

    @Test
    void stock_indicators_are_not_saved_when_any_final_bar_is_incomplete() {
        List<PriceObservation> observations = new ArrayList<>(stockObservations(120, TARGET_DATE));
        PriceObservation incomplete = observations.get(20);
        observations.set(20, new PriceObservation(incomplete.stockId(), incomplete.tradeDate(),
                incomplete.quoteScope(), incomplete.priceBasis(), incomplete.openPrice(), null,
                incomplete.lowPrice(), incomplete.closePrice(), incomplete.volume(), incomplete.tradingAmount(),
                incomplete.dataStatus(), incomplete.dataRevision()));
        given(stockPrices.recent(Set.of(7L), "KRX", TARGET_DATE, 252, PriceBasis.RAW))
                .willReturn(Map.of(7L, observations));

        service.calculateStockIndicators(7L, "KRX", TARGET_DATE);

        verify(indicators, never()).upsertStock(any());
    }

    @Test
    void stock_indicators_are_not_saved_from_a_provisional_observation() {
        List<PriceObservation> observations = new ArrayList<>(stockObservations(120, TARGET_DATE));
        PriceObservation target = observations.getLast();
        observations.set(observations.size() - 1, new PriceObservation(target.stockId(), target.tradeDate(),
                target.quoteScope(), target.priceBasis(), target.openPrice(), target.highPrice(), target.lowPrice(),
                target.closePrice(), target.volume(), target.tradingAmount(), "PROVISIONAL", target.dataRevision()));
        given(stockPrices.recent(Set.of(7L), "KRX", TARGET_DATE, 252, PriceBasis.RAW))
                .willReturn(Map.of(7L, observations));

        service.calculateStockIndicators(7L, "KRX", TARGET_DATE);

        verify(indicators, never()).upsertStock(any());
    }

    @Test
    void stock_indicators_are_not_saved_from_a_zero_price_bar() {
        List<PriceObservation> observations = new ArrayList<>(stockObservations(120, TARGET_DATE));
        PriceObservation target = observations.getLast();
        observations.set(observations.size() - 1, new PriceObservation(target.stockId(), target.tradeDate(),
                target.quoteScope(), target.priceBasis(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, target.volume(), target.tradingAmount(), target.dataStatus(), target.dataRevision()));
        given(stockPrices.recent(Set.of(7L), "KRX", TARGET_DATE, 252, PriceBasis.RAW))
                .willReturn(Map.of(7L, observations));

        service.calculateStockIndicators(7L, "KRX", TARGET_DATE);

        verify(indicators, never()).upsertStock(any());
    }

    @Test
    void index_indicators_use_only_final_complete_observations_and_include_target_day() {
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252))
                .willReturn(indexObservations(120, TARGET_DATE));

        service.calculateIndexIndicators(11L, TARGET_DATE);

        ArgumentCaptor<IndexTechnicalIndicator> saved = ArgumentCaptor.forClass(IndexTechnicalIndicator.class);
        verify(indicators).upsertIndex(saved.capture());
        assertThat(saved.getValue().tradeDate()).isEqualTo(TARGET_DATE);
        assertThat(saved.getValue().ma120()).isEqualByComparingTo("159.5000");
        assertThat(saved.getValue().inputManifest()).contains(TARGET_DATE.toString(), "\"revision\":1");
    }

    @Test
    void index_indicators_are_not_saved_when_final_history_is_insufficient() {
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252))
                .willReturn(indexObservations(119, TARGET_DATE));

        service.calculateIndexIndicators(11L, TARGET_DATE);

        verify(indicators, never()).upsertIndex(any());
    }

    @Test
    void index_indicators_are_not_saved_from_a_zero_price_bar() {
        List<IndexPriceObservation> observations = new ArrayList<>(indexObservations(120, TARGET_DATE));
        IndexPriceObservation target = observations.getLast();
        observations.set(observations.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, target.advanceCount(), target.declineCount(),
                target.unchangedCount(), target.upperLimitCount(), target.lowerLimitCount(), target.revision(),
                target.dataStatus()));
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252)).willReturn(observations);

        service.calculateIndexIndicators(11L, TARGET_DATE);

        verify(indicators, never()).upsertIndex(any());
    }

    @Test
    void index_weather_requires_final_target_breadth_and_uses_the_target_day() {
        List<IndexPriceObservation> observations = indexObservations(24, TARGET_DATE, true);
        assertThat(observations.getLast().tradeDate()).isEqualTo(TARGET_DATE);
        var indicators = org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator.calculateSeries(
                observations.stream().map(IndexPriceObservation::highPrice).toList(),
                observations.stream().map(IndexPriceObservation::lowPrice).toList(),
                observations.stream().map(IndexPriceObservation::closePrice).toList(),
                observations.stream().map(IndexPriceObservation::tradeDate).toList()).getLast();
        assertThat(indicators.getMa20()).isNotNull();
        assertThat(indicators.getRsi14()).isNotNull();
        assertThat(observations.getLast().advanceCount()).isNotNull();
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252)).willReturn(observations);

        service.calculateIndexWeather(11L, TARGET_DATE);

        org.mockito.ArgumentCaptor<IndexWeather> saved = org.mockito.ArgumentCaptor.forClass(IndexWeather.class);
        verify(indexWeather).upsert(saved.capture());
        assertThat(saved.getValue().tradeDate()).isEqualTo(TARGET_DATE);
        assertThat(saved.getValue().status()).isEqualTo("FINAL");
        assertThat(saved.getValue().score()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(saved.getValue().inputManifest()).contains(TARGET_DATE.toString(), "advanceCount",
                "\"advanceDeclineRatio\":243.14");
    }

    @Test
    void index_weather_is_not_saved_without_target_breadth_or_24_final_prices() {
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252))
                .willReturn(indexObservations(120, TARGET_DATE, false));

        service.calculateIndexWeather(11L, TARGET_DATE);

        verify(indexWeather, never()).upsert(any());
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252))
                .willReturn(indexObservations(23, TARGET_DATE, true));

        service.calculateIndexWeather(11L, TARGET_DATE);

        verify(indexWeather, never()).upsert(any());
    }

    @Test
    void index_weather_is_not_saved_from_provisional_index_prices() {
        List<IndexPriceObservation> observations = new ArrayList<>(indexObservations(120, TARGET_DATE, true));
        IndexPriceObservation target = observations.getLast();
        observations.set(observations.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), target.advanceCount(),
                target.declineCount(), target.unchangedCount(), target.upperLimitCount(), target.lowerLimitCount(),
                target.revision(), "PROVISIONAL"));
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252)).willReturn(observations);

        service.calculateIndexWeather(11L, TARGET_DATE);

        verify(indexWeather, never()).upsert(any());
    }

    @Test
    void index_weather_is_not_saved_when_breadth_counts_are_all_zero() {
        List<IndexPriceObservation> observations = indexObservations(24, TARGET_DATE, true).stream()
                .map(value -> new IndexPriceObservation(value.seriesId(), value.tradeDate(), value.highPrice(),
                        value.lowPrice(), value.closePrice(), 0, 0, 0, 0, 0, value.revision(), value.dataStatus()))
                .toList();
        given(indexPrices.recentFinal(11L, TARGET_DATE, 252)).willReturn(observations);

        service.calculateIndexWeather(11L, TARGET_DATE);

        verify(indexWeather, never()).upsert(any());
    }

    @Test
    void sector_leaders_prefer_final_gainers_then_rank_by_trading_amount() {
        given(leadingStocks.finalCandidates(11L, TARGET_DATE)).willReturn(List.of(
                leader(1, "1000", "1100", "1000"),
                leader(2, "1000", "900", "10000"),
                leader(3, "1000", "1200", "3000"),
                leader(4, "1000", "1100", "2000")));

        service.calculateSectorLeadingStocks(11L, TARGET_DATE);

        org.mockito.ArgumentCaptor<List<SectorLeadingStockPort.RankedSectorStock>> ranked =
                org.mockito.ArgumentCaptor.captor();
        verify(leadingStocks).replace(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(TARGET_DATE), ranked.capture(),
                org.mockito.ArgumentMatchers.contains("sector-leading-turnover-gainers-v1"));
        assertThat(ranked.getValue()).extracting(SectorLeadingStockPort.RankedSectorStock::stockId)
                .containsExactly(3L, 4L, 1L);
        assertThat(ranked.getValue()).extracting(SectorLeadingStockPort.RankedSectorStock::rank)
                .containsExactly(1, 2, 3);
    }

    @Test
    void sector_leaders_keep_negative_returns_when_no_final_gainers_exist() {
        given(leadingStocks.finalCandidates(11L, TARGET_DATE)).willReturn(List.of(
                leader(1, "1000", "900", "1000"), leader(2, "1000", "800", "2000")));

        service.calculateSectorLeadingStocks(11L, TARGET_DATE);

        verify(leadingStocks).replace(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(TARGET_DATE), any(), any());
    }

    @Test
    void sector_leaders_are_not_saved_without_complete_target_day_inputs() {
        given(leadingStocks.finalCandidates(11L, TARGET_DATE)).willReturn(List.of(
                new SectorLeadingStockCandidate(1, TARGET_DATE.minusDays(1), "KRX",
                        BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, 1, 1),
                new SectorLeadingStockCandidate(2, TARGET_DATE, "KRX",
                        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN, 1, 1)));

        service.calculateSectorLeadingStocks(11L, TARGET_DATE);

        verify(leadingStocks, never()).replace(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void new_listing_without_a_prior_final_close_is_not_ranked() {
        given(leadingStocks.finalCandidates(11L, TARGET_DATE)).willReturn(List.of(
                new SectorLeadingStockCandidate(8, TARGET_DATE, "KRX", null,
                        BigDecimal.TEN, BigDecimal.TEN, 1, 1)));

        service.calculateSectorLeadingStocks(11L, TARGET_DATE);

        verify(leadingStocks, never()).replace(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private static List<PriceObservation> stockObservations(int count, LocalDate lastDate) {
        List<PriceObservation> values = new ArrayList<>();
        List<LocalDate> dates = tradingDates(count, lastDate);
        for (int index = 0; index < dates.size(); index++) {
            LocalDate date = dates.get(index);
            BigDecimal close = BigDecimal.valueOf(100 + index);
            values.add(new PriceObservation(7L, date, "KRX", PriceBasis.RAW,
                    close, close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close,
                    10L, BigDecimal.valueOf(1000), "FINAL", 1));
        }
        return values;
    }

    private static List<IndexPriceObservation> indexObservations(int count, LocalDate lastDate) {
        return indexObservations(count, lastDate, false);
    }

    private static List<IndexPriceObservation> indexObservations(int count, LocalDate lastDate, boolean withBreadth) {
        List<IndexPriceObservation> values = new ArrayList<>();
        List<LocalDate> dates = tradingDates(count, lastDate);
        for (int index = 0; index < dates.size(); index++) {
            LocalDate date = dates.get(index);
            BigDecimal close = BigDecimal.valueOf(100 + index);
            values.add(new IndexPriceObservation(11L, date,
                    close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close,
                    withBreadth ? 100 + index : null, withBreadth ? 50 : null, withBreadth ? 10 : null,
                    withBreadth ? 1 : null, withBreadth ? 1 : null, 1, "FINAL"));
        }
        return values;
    }

    private static List<LocalDate> tradingDates(int count, LocalDate lastDate) {
        List<LocalDate> reversed = new ArrayList<>();
        for (LocalDate date = lastDate; reversed.size() < count; date = date.minusDays(1)) {
            if (date.getDayOfWeek().getValue() < 6 && !date.equals(LocalDate.of(2026, 9, 29))) {
                reversed.add(date);
            }
        }
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private static SectorLeadingStockCandidate leader(long stockId, String previousClose,
            String close, String tradingAmount) {
        return new SectorLeadingStockCandidate(stockId, TARGET_DATE, "KRX", new BigDecimal(previousClose),
                new BigDecimal(close), new BigDecimal(tradingAmount), 1, 1);
    }
}
