package org.stockwellness.application.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisOverseasIndexDailyPrice;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.application.port.out.external.kis.KisHolidayPort;
import org.stockwellness.application.port.out.external.kis.KisIndexPricePort;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.EodStockInvestorTradeDaily;
import org.stockwellness.domain.stock.price.InvestorSourceType;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NewMarketDataSyncServiceTest {
    @Mock KisDailyPricePort prices;
    @Mock KisIndexPricePort indexPrices;
    @Mock KisHolidayPort holidays;
    @Mock EodStockPricePort eod;
    @Mock EodStockInvestorTradePort investorTrades;
    @Mock IndexDailyPort indexDaily;
    @Mock TradingCalendarPort calendar;
    NewMarketDataSyncService service;

    @BeforeEach void setUp() {
        service = new NewMarketDataSyncService(prices, indexPrices, holidays, eod, investorTrades, indexDaily, calendar);
    }

    @Test
    void requests_prices_only_after_every_day_in_the_range_is_confirmed_by_the_calendar() {
        LocalDate start = LocalDate.of(2026, 10, 2);
        LocalDate closed = start.plusDays(1);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", start, closed)).willReturn(List.of(
                TradingCalendar.of("KOSPI", start, true, null, null, "test"),
                TradingCalendar.of("KOSPI", closed, false, null, null, "test")));
        given(prices.fetchDailyPrices(stock, start, start)).willReturn(List.of(price(start, "1", "N")));

        service.syncStockPrices(stock, start, closed);

        verify(prices).fetchDailyPrices(stock, start, start);
        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(start);
            assertThat(row.getId().getQuoteScope()).isEqualTo("KRX");
            assertThat(row.getDataStatus()).isEqualTo("PROVISIONAL");
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:RAW");
        });
    }

    @Test
    void unknown_calendar_stops_before_calling_the_price_provider() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of());

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");

        verify(prices, never()).fetchDailyPrices(stock, date, date);
        verify(eod, never()).upsertObservations(anyList(), anyList());
    }

    @Test
    void missing_open_day_response_is_persisted_as_incomplete_instead_of_zero() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of());

        service.syncStockPrices(stock, date, date);

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getDataStatus()).isEqualTo("INCOMPLETE");
            assertThat(row.getClosePrice()).isNull();
            assertThat(row.getVolume()).isNull();
        });
    }

    @Test
    void partial_quote_keeps_missing_values_null_and_marks_incomplete() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of(
                new KisDailyPriceDetail(date, new BigDecimal("10"), null, new BigDecimal("8"),
                        new BigDecimal("9"), 10L, new BigDecimal("100"), null, null, "N", null, null, null)));

        service.syncStockPrices(stock, date, date);

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getDataStatus()).isEqualTo("INCOMPLETE");
            assertThat(row.getHighPrice()).isNull();
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:RAW");
        });
    }

    @Test
    void malformed_price_precision_fails_the_collection_unit_without_writing() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of(
                price(date, "1.00001", "N")));

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precision");

        verify(eod, never()).upsertObservations(anyList(), anyList());
    }

    @Test
    void stock_requests_are_split_into_at_most_one_hundred_calendar_day_windows() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate middle = start.plusDays(99);
        LocalDate end = start.plusDays(100);
        Stock stock = stock();
        var days = java.util.stream.IntStream.rangeClosed(0, 100)
                .mapToObj(offset -> TradingCalendar.of("KOSPI", start.plusDays(offset), true, null, null, "test"))
                .toList();
        given(calendar.findAll("KOSPI", start, end)).willReturn(days);
        given(prices.fetchDailyPrices(stock, start, middle)).willReturn(List.of());
        given(prices.fetchDailyPrices(stock, end, end)).willReturn(List.of());

        service.syncStockPrices(stock, start, end);

        verify(prices).fetchDailyPrices(stock, start, middle);
        verify(prices).fetchDailyPrices(stock, end, end);
        verify(prices, times(2)).fetchDailyPrices(org.mockito.ArgumentMatchers.eq(stock),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void raw_and_provider_adjusted_quotes_are_fetched_and_saved_as_separate_observations() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of(price(date, "100", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, date, date)).willReturn(List.of(price(date, "95", "N")));

        service.syncStockPrices(stock, date, date);

        ArgumentCaptor<List<EodStockPrice>> raw = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<StockPriceAdjustment>> adjusted = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(raw.capture(), adjusted.capture());
        assertThat(raw.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getClosePrice()).isEqualByComparingTo("100");
            assertThat(row.getDataStatus()).isEqualTo("PROVISIONAL");
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:RAW");
        });
        assertThat(adjusted.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getAdjustedClose()).isEqualByComparingTo("95");
            assertThat(row.getAdjustmentAsOf()).isAfterOrEqualTo(date);
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:ADJUSTED");
        });
    }

    @Test
    void adjustment_provider_failure_prevents_persisting_the_raw_observation() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of(price(date, "100", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, date, date)).willThrow(new IllegalStateException("provider unavailable"));

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("provider unavailable");

        verify(eod, never()).upsertObservations(anyList(), anyList());
    }

    @Test
    void unsupported_total_return_series_fails_without_price_fallback() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        IndexSeries totalReturn = new IndexSeries(MarketIndex.of("0001", "코스피", "0"), "KIS",
                "kis.total-return", "0001.TR", "KOSPI", "TOTAL_RETURN", "GROSS", "KRW", true);

        assertThatThrownBy(() -> service.syncIndexPrices(totalReturn, date, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active canonical KIS KRW price");

        verify(calendar, never()).findAll("KOSPI", date, date);
        verify(indexPrices, never()).fetchRawIndexDailyPrices(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(indexDaily, never()).upsert(anyList());
    }

    @Test
    void successful_post_close_kis_sample_without_finality_flag_stays_provisional() throws IOException {
        var resource = getClass().getResourceAsStream("/kis/source-contract/domestic-daily-success.json");
        assertThat(resource).as("sanitized KIS response fixture").isNotNull();
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var fixture = mapper.readTree(resource);
        var response = fixture.path("response");
        assertThat(response.path("rt_cd").asText()).isEqualTo("0");
        var row = mapper.treeToValue(response.path("output2").get(0), KisDailyPriceDetail.class);
        assertThat(row.baseDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(row.modYn()).isEqualTo("N");
        assertThat(Instant.parse(fixture.path("collected_at").asText()))
                .isAfter(row.baseDate().atTime(LocalTime.of(15, 30)).atZone(ZoneId.of("Asia/Seoul")).toInstant());

        Stock stock = stock();
        given(calendar.findAll("KOSPI", row.baseDate(), row.baseDate())).willReturn(List.of(
                TradingCalendar.of("KOSPI", row.baseDate(), true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchDailyPrices(stock, row.baseDate(), row.baseDate())).willReturn(List.of(row));
        given(prices.fetchAdjustedDailyPrices(stock, row.baseDate(), row.baseDate())).willReturn(List.of());

        service.syncStockPrices(stock, row.baseDate(), row.baseDate());

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(price ->
                assertThat(price.getDataStatus()).isEqualTo("PROVISIONAL"));
    }

    @Test
    void verified_market_sessions_cover_krx_holiday_us_dst_and_early_close() throws IOException {
        var resource = getClass().getResourceAsStream("/kis/source-contract/market-sessions-2026.json");
        assertThat(resource).as("official exchange session fixture").isNotNull();
        var fixture = new ObjectMapper().readTree(resource);

        var krxHoliday = session(fixture, "KOSPI", "2026-05-01");
        assertThat(krxHoliday.path("is_trading_day").asBoolean()).isFalse();
        assertThat(krxHoliday.path("open_local").isNull()).isTrue();
        var krxOpen = session(fixture, "KOSPI", "2026-05-04");
        assertThat(krxOpen.path("close_local").asText()).isEqualTo("15:30");
        assertThat(session(fixture, "US_EQUITY", "2026-04-03").path("is_trading_day").asBoolean()).isFalse();
        assertThat(session(fixture, "US_EQUITY", "2026-11-27").path("close_local").asText()).isEqualTo("13:00");
        assertThat(session(fixture, "US_EQUITY", "2026-03-06").path("open_utc").asText()).isEqualTo("2026-03-06T14:30:00Z");
        assertThat(session(fixture, "US_EQUITY", "2026-03-09").path("open_utc").asText()).isEqualTo("2026-03-09T13:30:00Z");

        for (JsonNode market : fixture.path("markets")) {
            ZoneId zone = ZoneId.of(market.path("timezone").asText());
            for (JsonNode day : market.path("sessions")) {
                if (!day.path("is_trading_day").asBoolean()) continue;
                LocalDate date = LocalDate.parse(day.path("date").asText());
                assertThat(date.atTime(LocalTime.parse(day.path("open_local").asText())).atZone(zone).toInstant())
                        .isEqualTo(Instant.parse(day.path("open_utc").asText()));
                assertThat(date.atTime(LocalTime.parse(day.path("close_local").asText())).atZone(zone).toInstant())
                        .isEqualTo(Instant.parse(day.path("close_utc").asText()));
            }
        }
    }

    @Test
    void investor_flows_are_saved_per_type_with_signed_values_and_missing_values_left_null() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of(investorRow(
                date, "-2", "-1.25", "3", "2.5", "0", "0", "1", "2", "1", "3",
                "1", "4", "1", "5", null, "6", "1", "7", "1", "8", "1", "9", "1", "10", "1", "11")));

        service.syncStockInvestorTrades(stock, date);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length);
        EodStockInvestorTradeDaily foreign = row(saved.getValue(), InvestorSourceType.FOREIGN);
        assertThat(foreign.netQuantity()).isEqualByComparingTo("-2");
        assertThat(foreign.netAmount()).isEqualByComparingTo("-1250000.00");
        assertThat(foreign.dataStatus()).isEqualTo("PROVISIONAL");
        assertThat(row(saved.getValue(), InvestorSourceType.INSTITUTION_TOTAL).netAmount())
                .isEqualByComparingTo("2500000.00");
        EodStockInvestorTradeDaily individual = row(saved.getValue(), InvestorSourceType.INDIVIDUAL);
        assertThat(individual.netQuantity()).isEqualByComparingTo("0");
        assertThat(individual.netAmount()).isEqualByComparingTo("0.00");
        EodStockInvestorTradeDaily bank = row(saved.getValue(), InvestorSourceType.BANK);
        assertThat(bank.netQuantity()).isNull();
        assertThat(bank.netAmount()).isEqualByComparingTo("6000000.00");
        assertThat(bank.dataStatus()).isEqualTo("INCOMPLETE");
        assertThat(saved.getValue()).allSatisfy(value -> {
            assertThat(value.tradeDate()).isEqualTo(date);
            assertThat(value.quoteScope()).isEqualTo("KRX");
            assertThat(value.source()).isEqualTo("KIS:FHPTJ04160001:J");
        });
    }

    @Test
    void absent_investor_response_is_persisted_as_incomplete_nulls_not_zero() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of());

        service.syncStockInvestorTrades(stock, date);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length).allSatisfy(value -> {
            assertThat(value.netQuantity()).isNull();
            assertThat(value.netAmount()).isNull();
            assertThat(value.dataStatus()).isEqualTo("INCOMPLETE");
        });
    }

    @Test
    void investor_response_with_another_business_date_is_rejected_without_writing() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of(investorRow(
                date.minusDays(1), "1", "1", "1", "1", "1", "1", "1", "1", "1", "1",
                "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1")));

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("date");
        verify(investorTrades, never()).upsert(anyList());
    }

    @Test
    void investor_quantity_precision_overflow_fails_before_writing_instead_of_rounding() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of(investorRow(
                date, "1.0000001", "1", "1", "1", "1", "1", "1", "1", "1", "1",
                "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1")));

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("storage scale");
        verify(investorTrades, never()).upsert(anyList());
    }

    @Test
    void domestic_index_prices_are_stored_as_provisional_and_keep_zero_values() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(indexPrices.fetchRawIndexDailyPrices("0001", date, date)).willReturn(List.of(
                new KisDailySectorDetail("20261005", "2500.00", "0", "3", "0", "0", "0",
                        "2490.00", "2510.00", "2480.00")));

        service.syncIndexPrices(domesticSeries(), date, date);

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getClosePrice()).isEqualByComparingTo("2500");
            assertThat(row.getVolume()).isZero();
            assertThat(row.getTradingAmount()).isEqualByComparingTo("0");
            assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.PROVISIONAL);
        });
    }

    @Test
    void missing_domestic_open_day_is_recorded_incomplete_without_synthetic_values() {
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = start.plusDays(1);
        given(calendar.findAll("KOSPI", start, end)).willReturn(List.of(
                TradingCalendar.of("KOSPI", start, true, null, null, "test"),
                TradingCalendar.of("KOSPI", end, true, null, null, "test")));
        given(indexPrices.fetchRawIndexDailyPrices("0001", start, end)).willReturn(List.of(
                new KisDailySectorDetail("20261005", "2500.00", "1", "2", "0.04", "100", "250000",
                        "2490.00", "2510.00", "2480.00")));

        service.syncIndexPrices(domesticSeries(), start, end);

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue()).filteredOn(row -> row.getId().getTradeDate().equals(end)).singleElement()
                .satisfies(row -> {
                    assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.INCOMPLETE);
                    assertThat(row.getClosePrice()).isNull();
                    assertThat(row.getVolume()).isNull();
                    assertThat(row.getTradingAmount()).isNull();
                });
    }

    @Test
    void supported_us_index_uses_the_overseas_endpoint_and_keeps_unreported_trading_amount_null() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        IndexSeries sp500 = usSeries("SPX", "NYSE");
        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", date, date)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20261005", "6750.25", "6730", "6760", "6700", "12345", "N")));

        service.syncIndexPrices(sp500, date, date);

        verify(indexPrices).fetchOverseasIndexDailyPrices("SPX", date, date);
        verify(indexPrices, never()).fetchRawIndexDailyPrices(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(date);
            assertThat(row.getClosePrice()).isEqualByComparingTo("6750.2500");
            assertThat(row.getVolume()).isEqualTo(12345L);
            assertThat(row.getTradingAmount()).isNull();
            assertThat(row.getPriceSourceApi()).isEqualTo("FHKST03030100");
            assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.PROVISIONAL);
        });
    }

    @Test
    void unsupported_us_index_is_rejected_before_calling_kis() {
        LocalDate date = LocalDate.of(2026, 10, 5);

        assertThatThrownBy(() -> service.syncIndexPrices(usSeries("COMP", "NASDAQ"), date, date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KIS-supported");

        verify(indexPrices, never()).fetchOverseasIndexDailyPrices(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(indexDaily, never()).upsert(anyList());
    }

    @Test
    void missing_us_volume_remains_null_and_marks_the_quote_incomplete() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", date, date)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20261005", "6750.25", "6730", "6760", "6700", " ", "N")));

        service.syncIndexPrices(usSeries("SPX", "NYSE"), date, date);

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getVolume()).isNull();
            assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.INCOMPLETE);
        });
    }

    @Test
    void eod_session_end_uses_krx_local_close_and_new_york_dst_rules() {
        LocalDate krDate = LocalDate.of(2026, 11, 13);
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "KOSPI", krDate, java.time.Instant.parse("2026-11-13T06:29:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "KOSPI", krDate, java.time.Instant.parse("2026-11-13T06:30:00Z"))).isTrue();

        LocalDate usSummerDate = LocalDate.of(2026, 7, 13);
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "NASDAQ", usSummerDate, java.time.Instant.parse("2026-07-13T19:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "NASDAQ", usSummerDate, java.time.Instant.parse("2026-07-13T20:00:00Z"))).isTrue();

        LocalDate usWinterDate = LocalDate.of(2026, 11, 13);
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "NYSE", usWinterDate, java.time.Instant.parse("2026-11-13T20:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                "NYSE", usWinterDate, java.time.Instant.parse("2026-11-13T21:00:00Z"))).isTrue();
    }

    private static JsonNode session(JsonNode fixture, String market, String date) {
        for (JsonNode day : fixture.path("markets").path(market).path("sessions")) {
            if (date.equals(day.path("date").asText())) return day;
        }
        throw new AssertionError("Missing session fixture: " + market + " " + date);
    }

    private static Stock stock() {
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.empty(), StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        return stock;
    }

    private static EodStockInvestorTradeDaily row(List<EodStockInvestorTradeDaily> values, InvestorSourceType type) {
        return values.stream().filter(value -> value.investorType() == type).findFirst().orElseThrow();
    }

    private static KisInvestorTradeByStockDaily investorRow(LocalDate date, String foreignQuantity, String foreignAmount,
            String institutionQuantity, String institutionAmount, String individualQuantity, String individualAmount,
            String financialQuantity, String financialAmount, String insuranceQuantity, String insuranceAmount,
            String trustQuantity, String trustAmount, String privateFundQuantity, String privateFundAmount,
            String bankQuantity, String bankAmount, String pensionQuantity, String pensionAmount,
            String merchantBankQuantity, String merchantBankAmount, String otherOrganizationQuantity,
            String otherOrganizationAmount, String otherCorporateQuantity, String otherCorporateAmount,
            String otherQuantity, String otherAmount) {
        return new KisInvestorTradeByStockDaily(date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                foreignQuantity, foreignAmount, institutionQuantity, institutionAmount,
                individualQuantity, individualAmount, financialQuantity, financialAmount,
                insuranceQuantity, insuranceAmount, trustQuantity, trustAmount,
                privateFundQuantity, privateFundAmount, bankQuantity, bankAmount,
                pensionQuantity, pensionAmount, merchantBankQuantity, merchantBankAmount,
                otherOrganizationQuantity, otherOrganizationAmount, otherCorporateQuantity, otherCorporateAmount,
                otherQuantity, otherAmount);
    }

    private static KisDailyPriceDetail price(LocalDate date, String close, String modYn) {
        BigDecimal closePrice = new BigDecimal(close);
        return new KisDailyPriceDetail(date, closePrice, closePrice.add(BigDecimal.ONE),
                closePrice.subtract(BigDecimal.ONE).max(BigDecimal.ZERO), closePrice, 10L, new BigDecimal("1000.00"),
                null, null, modYn, null, null, null);
    }

    private static IndexSeries usSeries(String code, String market) {
        return new IndexSeries(new MarketIndex("US:" + code, code), "KIS", "kis.ovrs.idxcode.N", code,
                market, "PRICE", "NONE", "USD", true);
    }

    private static IndexSeries domesticSeries() {
        return new IndexSeries(MarketIndex.of("0001", "코스피", "0"), "KIS", "kis.idxcode.KOSPI", "0001",
                "KOSPI", "PRICE", "NONE", "KRW", true);
    }
}
