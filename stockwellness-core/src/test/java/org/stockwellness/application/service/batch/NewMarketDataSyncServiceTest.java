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
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.application.port.out.external.kis.KisHolidayPort;
import org.stockwellness.application.port.out.external.kis.KisIndexPricePort;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
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

@ExtendWith(MockitoExtension.class)
class NewMarketDataSyncServiceTest {
    @Mock KisDailyPricePort prices;
    @Mock KisIndexPricePort indexPrices;
    @Mock KisHolidayPort holidays;
    @Mock EodStockPricePort eod;
    @Mock IndexDailyPort indexDaily;
    @Mock TradingCalendarPort calendar;
    NewMarketDataSyncService service;

    @BeforeEach void setUp() {
        service = new NewMarketDataSyncService(prices, indexPrices, holidays, eod, indexDaily, calendar);
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
        verify(eod).upsert(saved.capture());
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
        verify(eod, never()).upsert(anyList());
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
        verify(eod).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getDataStatus()).isEqualTo("INCOMPLETE");
            assertThat(row.getClosePrice()).isNull();
            assertThat(row.getVolume()).isNull();
        });
        verify(eod, never()).upsertAdjustments(anyList());
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
        verify(eod).upsert(saved.capture());
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

        verify(eod, never()).upsert(anyList());
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
        verify(eod).upsert(raw.capture());
        assertThat(raw.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getClosePrice()).isEqualByComparingTo("100");
            assertThat(row.getDataStatus()).isEqualTo("PROVISIONAL");
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:RAW");
        });
        ArgumentCaptor<List<StockPriceAdjustment>> adjusted = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertAdjustments(adjusted.capture());
        assertThat(adjusted.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getAdjustedClose()).isEqualByComparingTo("95");
            assertThat(row.getAdjustmentAsOf()).isAfterOrEqualTo(date);
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:ADJUSTED");
        });
    }

    @Test
    void adjustment_provider_failure_does_not_rollback_the_raw_observation() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, date, date)).willReturn(List.of(price(date, "100", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, date, date)).willThrow(new IllegalStateException("provider unavailable"));

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("provider unavailable");

        verify(eod).upsert(anyList());
        verify(eod, never()).upsertAdjustments(anyList());
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

    private static JsonNode session(JsonNode fixture, String market, String date) {
        for (JsonNode day : fixture.path("markets").path(market).path("sessions")) {
            if (date.equals(day.path("date").asText())) return day;
        }
        throw new AssertionError("Missing session fixture: " + market + " " + date);
    }

    private static Stock stock() {
        return Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.empty(), StockStatus.ACTIVE);
    }

    private static KisDailyPriceDetail price(LocalDate date, String close, String modYn) {
        BigDecimal closePrice = new BigDecimal(close);
        return new KisDailyPriceDetail(date, closePrice, closePrice.add(BigDecimal.ONE),
                closePrice.subtract(BigDecimal.ONE).max(BigDecimal.ZERO), closePrice, 10L, new BigDecimal("1000.00"),
                null, null, modYn, null, null, null);
    }
}
