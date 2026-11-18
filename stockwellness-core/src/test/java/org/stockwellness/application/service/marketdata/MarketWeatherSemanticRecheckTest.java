package org.stockwellness.application.service.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.application.port.out.stock.IndexWeatherPort;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.application.port.out.stock.TechnicalIndicatorPort;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.insight.IndexWeather;
import org.stockwellness.domain.stock.insight.MarketWeatherPolicy;
import org.stockwellness.domain.stock.insight.RollingPercentileCalculator;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

@ExtendWith(MockitoExtension.class)
class MarketWeatherSemanticRecheckTest {
    private static final long SERIES_ID = 11L;
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 5);
    private static final List<Integer> CLOSES = List.of(
            100, 101, 99, 102, 101, 103, 100, 104, 102, 105, 103, 106,
            104, 107, 105, 108, 106, 109, 107, 110, 108, 111, 109, 112);

    @Mock private StockPriceReadPort stockPrices;
    @Mock private IndexDailyReadPort indexPrices;
    @Mock private IndexWeatherPort indexWeather;
    @Mock private TechnicalIndicatorPort indicators;
    @Mock private SectorLeadingStockPort leadingStocks;
    private MarketDerivedDataService service;

    @BeforeEach
    void setUp() {
        service = new MarketDerivedDataService(stockPrices, indexPrices, indexWeather, indicators, leadingStocks,
                new ObjectMapper());
    }

    @Test
    void normal_final_fixture_matches_legacy_sector_weather_score() throws Exception {
        List<IndexPriceObservation> fixture = observations(false);
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        ArgumentCaptor<IndexWeather> saved = ArgumentCaptor.forClass(IndexWeather.class);
        verify(indexWeather).upsert(saved.capture());
        IndexWeather result = saved.getValue();
        JsonNode manifest = new ObjectMapper().readTree(result.inputManifest());
        JsonNode observations = manifest.path("observations");
        JsonNode current = observations.get(4);
        IndexPriceObservation target = fixture.getLast();
        TechnicalIndicators targetIndicators = TechnicalIndicatorCalculator.calculateSeries(
                fixture.stream().map(IndexPriceObservation::closePrice).toList()).getLast();
        BigDecimal oldTrend = target.closePrice().divide(targetIndicators.getMa20(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal canonicalTrend = target.closePrice().divide(targetIndicators.getMa20(), 6, RoundingMode.HALF_UP);

        assertThat(result.score()).isEqualByComparingTo(BigDecimal.valueOf(legacyScore(fixture)));
        assertThat(result.score()).isEqualByComparingTo("70.00");
        assertThat(oldTrend).isEqualByComparingTo("105.6600");
        assertThat(canonicalTrend).isEqualByComparingTo("1.056604");
        assertThat(current.path("ma20Disparity").decimalValue()).isEqualByComparingTo(canonicalTrend);
        assertThat(current.path("advanceDeclineRatio").decimalValue()).isEqualByComparingTo(legacyAdr(target));
        assertThat(current.path("advanceDeclineRatio").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(current.path("rsi14").decimalValue()).isEqualByComparingTo("61.9982");
        BigDecimal scaledCanonicalTrend = canonicalTrend.multiply(BigDecimal.valueOf(100));
        assertThat(oldTrend).isNotEqualByComparingTo(scaledCanonicalTrend);
        assertThat(oldTrend.subtract(scaledCanonicalTrend).abs()).isLessThan(new BigDecimal("0.01"));
        assertThat(observations.size()).isEqualTo(5);
        assertThat(result.status()).isEqualTo("FINAL");
        assertThat(result.scoreVersion()).isEqualTo("market-weather-v1");
        assertThat(result.inputManifest()).contains("\"date\":\"2026-10-05\"");
    }

    @Test
    void missing_unchanged_count_still_calculates_legacy_adr() throws Exception {
        List<IndexPriceObservation> fixture = observations(true);
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        ArgumentCaptor<IndexWeather> saved = ArgumentCaptor.forClass(IndexWeather.class);
        verify(indexWeather).upsert(saved.capture());
        JsonNode manifest = new ObjectMapper().readTree(saved.getValue().inputManifest());
        JsonNode observations = manifest.path("observations");
        JsonNode target = observations.get(observations.size() - 1);
        assertThat(saved.getValue().score()).isEqualByComparingTo(BigDecimal.valueOf(legacyScore(fixture)));
        assertThat(target.path("unchangedCount").isMissingNode()).isTrue();
        assertThat(target.path("advanceDeclineRatio").decimalValue()).isEqualByComparingTo("150.00");
    }

    @ParameterizedTest
    @MethodSource("requiredBreadthFields")
    void missing_required_target_breadth_count_blocks_weather(String field) {
        List<IndexPriceObservation> fixture = observations(false);
        fixture.set(fixture.size() - 1, withCount(fixture.getLast(), field, null));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @MethodSource("requiredBreadthFields")
    void zero_required_target_breadth_count_is_valid_when_the_other_counts_exist(String field) throws Exception {
        List<IndexPriceObservation> fixture = observations(false);
        fixture.set(fixture.size() - 1, withCount(fixture.getLast(), field, 0));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        ArgumentCaptor<IndexWeather> saved = ArgumentCaptor.forClass(IndexWeather.class);
        verify(indexWeather).upsert(saved.capture());
        JsonNode manifest = new ObjectMapper().readTree(saved.getValue().inputManifest());
        JsonNode observations = manifest.path("observations");
        assertThat(observations.get(observations.size() - 1).path(field).intValue()).isZero();
    }

    @ParameterizedTest
    @MethodSource("requiredBreadthFields")
    void negative_required_target_breadth_count_blocks_weather(String field) {
        List<IndexPriceObservation> fixture = observations(false);
        fixture.set(fixture.size() - 1, withCount(fixture.getLast(), field, -1));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void negative_optional_unchanged_count_blocks_weather() {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), target.advanceCount(),
                target.declineCount(), -1, target.upperLimitCount(), target.lowerLimitCount(),
                target.revision(), target.dataStatus()));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmed_zero_unchanged_count_is_saved_in_the_weather_manifest() throws Exception {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), target.advanceCount(),
                target.declineCount(), 0, target.upperLimitCount(), target.lowerLimitCount(),
                target.revision(), target.dataStatus()));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        ArgumentCaptor<IndexWeather> saved = ArgumentCaptor.forClass(IndexWeather.class);
        verify(indexWeather).upsert(saved.capture());
        JsonNode manifest = new ObjectMapper().readTree(saved.getValue().inputManifest());
        JsonNode stored = manifest.path("observations").get(manifest.path("observations").size() - 1);
        assertThat(stored.path("unchangedCount").intValue()).isZero();
    }

    @Test
    void future_index_price_row_cannot_supply_target_day_breadth() {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), TARGET_DATE.plusDays(1),
                target.highPrice(), target.lowPrice(), target.closePrice(), target.advanceCount(),
                target.declineCount(), target.unchangedCount(), target.upperLimitCount(), target.lowerLimitCount(),
                target.revision(), target.dataStatus()));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void provisional_target_price_cannot_publish_final_weather() {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), target.advanceCount(),
                target.declineCount(), target.unchangedCount(), target.upperLimitCount(), target.lowerLimitCount(),
                target.revision(), "PROVISIONAL"));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void all_zero_required_counts_need_a_confirmed_unchanged_count() {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), 0, 0, null, 0, 0,
                target.revision(), target.dataStatus()));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void all_zero_counts_do_not_form_a_valid_breadth_observation() {
        List<IndexPriceObservation> fixture = observations(false);
        IndexPriceObservation target = fixture.getLast();
        fixture.set(fixture.size() - 1, new IndexPriceObservation(target.seriesId(), target.tradeDate(),
                target.highPrice(), target.lowPrice(), target.closePrice(), 0, 0, 0, 0, 0,
                target.revision(), target.dataStatus()));
        given(indexPrices.recentFinal(SERIES_ID, TARGET_DATE, 252)).willReturn(fixture);

        service.calculateIndexWeather(SERIES_ID, TARGET_DATE);

        verify(indexWeather, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    private static Stream<String> requiredBreadthFields() {
        return Stream.of("advanceCount", "declineCount", "upperLimitCount", "lowerLimitCount");
    }

    private static IndexPriceObservation withCount(IndexPriceObservation value, String field, Integer count) {
        Integer advance = "advanceCount".equals(field) ? count : value.advanceCount();
        Integer decline = "declineCount".equals(field) ? count : value.declineCount();
        Integer upperLimit = "upperLimitCount".equals(field) ? count : value.upperLimitCount();
        Integer lowerLimit = "lowerLimitCount".equals(field) ? count : value.lowerLimitCount();
        return new IndexPriceObservation(value.seriesId(), value.tradeDate(), value.highPrice(), value.lowPrice(),
                value.closePrice(), advance, decline, value.unchangedCount(), upperLimit, lowerLimit,
                value.revision(), value.dataStatus());
    }

    private static List<IndexPriceObservation> observations(boolean missingTargetUnchanged) {
        List<IndexPriceObservation> values = new ArrayList<>();
        for (int i = 0; i < CLOSES.size(); i++) {
            BigDecimal close = BigDecimal.valueOf(CLOSES.get(i));
            Integer unchanged = missingTargetUnchanged && i == CLOSES.size() - 1 ? null : 2 + i % 3;
            values.add(new IndexPriceObservation(SERIES_ID, TARGET_DATE.minusDays(CLOSES.size() - 1L - i),
                    close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close,
                    8 + i % 4, 5 + (i * 2) % 5, unchanged, i % 2, i % 3, 1, "FINAL"));
        }
        return values;
    }

    private static int legacyScore(List<IndexPriceObservation> observations) {
        // This test oracle follows the 99df1303 SectorIndicatorMapper and WeatherScoreCalculator formulas.
        List<BigDecimal> closes = observations.stream().map(IndexPriceObservation::closePrice).toList();
        List<TechnicalIndicators> technical = TechnicalIndicatorCalculator.calculateSeries(closes);
        List<BigDecimal> trend = new ArrayList<>();
        List<BigDecimal> breadth = new ArrayList<>();
        List<BigDecimal> momentum = new ArrayList<>();
        for (int i = 0; i < observations.size(); i++) {
            TechnicalIndicators indicators = technical.get(i);
            if (indicators.getMa20() == null || indicators.getRsi14() == null) continue;
            IndexPriceObservation value = observations.get(i);
            trend.add(value.closePrice().divide(indicators.getMa20(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
            breadth.add(legacyAdr(value));
            momentum.add(indicators.getRsi14());
        }
        int last = trend.size() - 1;
        return MarketWeatherPolicy.DEFAULT.calculateIntegratedScore(
                RollingPercentileCalculator.calculate(trend.get(last), trend),
                RollingPercentileCalculator.calculate(breadth.get(last), breadth),
                RollingPercentileCalculator.calculate(momentum.get(last), momentum));
    }

    private static BigDecimal legacyAdr(IndexPriceObservation value) {
        long advancing = value.advanceCount() + (long) value.upperLimitCount();
        long declining = value.declineCount() + (long) value.lowerLimitCount();
        if (advancing == 0 && declining == 0) return BigDecimal.valueOf(100);
        return BigDecimal.valueOf(advancing).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(Math.max(declining, 1)), 2, RoundingMode.HALF_UP);
    }
}
