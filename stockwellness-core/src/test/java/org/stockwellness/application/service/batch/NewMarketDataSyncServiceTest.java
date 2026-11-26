package org.stockwellness.application.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.service.marketdata.UsMarketCalendar;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorTradeByStockDaily;
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
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
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
    void listed_stock_price_range_ignores_open_sessions_before_listing_date() {
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate listed = start.plusDays(2);
        LocalDate end = listed.plusDays(1);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "listingDate", listed);
        given(calendar.findAll("KOSPI", start, end)).willReturn(List.of(
                TradingCalendar.of("KOSPI", start, true, null, null, "test"),
                TradingCalendar.of("KOSPI", start.plusDays(1), true, null, null, "test"),
                TradingCalendar.of("KOSPI", listed, true, null, null, "test"),
                TradingCalendar.of("KOSPI", end, true, null, null, "test")));
        given(prices.fetchDailyPrices(stock, listed, end)).willReturn(List.of(
                price(listed, "10", "N"), price(end, "11", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, listed, end)).willReturn(List.of(
                price(listed, "9", "N"), price(end, "10", "N")));

        service.syncStockPrices(stock, start, end);

        verify(prices).fetchDailyPrices(stock, listed, end);
        verify(prices).fetchAdjustedDailyPrices(stock, listed, end);
        ArgumentCaptor<List<EodStockPrice>> raw = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<StockPriceAdjustment>> adjusted = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(raw.capture(), adjusted.capture());
        assertThat(raw.getValue()).extracting(row -> row.getId().getTradeDate()).containsExactly(listed, end);
        assertThat(adjusted.getValue()).extracting(row -> row.getId().getTradeDate()).containsExactly(listed, end);
    }

    @Test
    void listed_stock_investor_range_ignores_open_sessions_before_listing_date() {
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate listed = start.plusDays(2);
        LocalDate end = listed.plusDays(1);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "listingDate", listed);
        given(calendar.findAll("KOSPI", start, end)).willReturn(List.of(
                TradingCalendar.of("KOSPI", start, true, null, null, "test"),
                TradingCalendar.of("KOSPI", start.plusDays(1), true, null, null, "test"),
                TradingCalendar.of("KOSPI", listed, true, null, null, "test"),
                TradingCalendar.of("KOSPI", end, true, null, null, "test")));
        given(prices.fetchInvestorTradeByStockDaily(stock, end)).willReturn(List.of(
                investorRow(end, "1", "1"), investorRow(listed, "1", "1"), investorRow(start, "1", "1")));

        service.syncStockInvestorTrades(stock, start, end);

        verify(prices).fetchInvestorTradeByStockDaily(stock, end);
        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(2 * InvestorSourceType.values().length)
                .extracting(EodStockInvestorTradeDaily::tradeDate).doesNotContain(start, start.plusDays(1));
        assertThat(saved.getValue().stream().map(EodStockInvestorTradeDaily::tradeDate).distinct().toList())
                .containsExactly(listed, end);
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

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

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

        assertThatThrownBy(() -> service.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

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

        assertThatThrownBy(() -> service.syncStockPrices(stock, start, end))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

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
                .hasMessageContaining("active canonical KIS");

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
        verify(eod).upsertObservations(saved.capture(), anyList());
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

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

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
    void investor_trade_range_uses_the_latest_date_window_and_skips_closed_days() {
        LocalDate first = LocalDate.of(2026, 10, 5);
        LocalDate closed = first.plusDays(1);
        LocalDate second = first.plusDays(2);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", first, second)).willReturn(List.of(
                TradingCalendar.of("KOSPI", first, true, null, null, "test"),
                TradingCalendar.of("KOSPI", closed, false, null, null, "test"),
                TradingCalendar.of("KOSPI", second, true, null, null, "test")));
        given(prices.fetchInvestorTradeByStockDaily(stock, second)).willReturn(List.of(
                investorRow(first, "-1", "2"), investorRow(second, "-2", "3")));

        service.syncStockInvestorTrades(stock, first, second);

        verify(prices, never()).fetchInvestorTradeByStockDaily(stock, first);
        verify(prices).fetchInvestorTradeByStockDaily(stock, second);
        verify(prices, never()).fetchInvestorTradeByStockDaily(stock, closed);
        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length * 2);
        assertThat(saved.getValue().stream().filter(row -> row.tradeDate().equals(first)).toList())
                .hasSize(InvestorSourceType.values().length).allSatisfy(row ->
                        assertThat(row.dataStatus()).isEqualTo("PROVISIONAL"));
        assertThat(row(saved.getValue(), InvestorSourceType.FOREIGN).netAmount()).isEqualByComparingTo("2000000.00");
        assertThat(saved.getValue().stream().filter(row -> row.tradeDate().equals(second)).toList())
                .hasSize(InvestorSourceType.values().length).allSatisfy(row ->
                        assertThat(row.dataStatus()).isEqualTo("PROVISIONAL"));
        assertThat(saved.getValue().stream()
                .filter(row -> row.tradeDate().equals(second) && row.investorType() == InvestorSourceType.FOREIGN)
                .findFirst().orElseThrow().netAmount()).isEqualByComparingTo("3000000.00");
    }

    @Test
    void investor_trade_range_requests_each_30_session_window_once() {
        LocalDate start = LocalDate.of(2026, 1, 5);
        Stock stock = stock();
        List<LocalDate> openDates = new java.util.ArrayList<>();
        List<TradingCalendar> calendarRows = new java.util.ArrayList<>();
        for (LocalDate date = start; openDates.size() < 31; date = date.plusDays(1)) {
            boolean open = date.getDayOfWeek().getValue() < 6;
            calendarRows.add(TradingCalendar.of("KOSPI", date, open, null, null, "test"));
            if (open) openDates.add(date);
        }
        LocalDate end = openDates.getLast();
        given(calendar.findAll("KOSPI", start, end)).willReturn(calendarRows);
        given(prices.fetchInvestorTradeByStockDaily(stock, openDates.get(29))).willReturn(
                openDates.subList(0, 30).stream().map(date -> investorRow(date, "1", "1")).toList());
        given(prices.fetchInvestorTradeByStockDaily(stock, end)).willReturn(
                openDates.subList(1, 31).stream().map(date -> investorRow(date, "1", "1")).toList());

        service.syncStockInvestorTrades(stock, start, end);

        verify(prices).fetchInvestorTradeByStockDaily(stock, openDates.get(29));
        verify(prices).fetchInvestorTradeByStockDaily(stock, end);
        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades, times(2)).upsert(saved.capture());
        assertThat(saved.getAllValues()).extracting(List::size)
                .containsExactly(30 * InvestorSourceType.values().length, InvestorSourceType.values().length);
        List<EodStockInvestorTradeDaily> allRows = saved.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(allRows.stream().map(EodStockInvestorTradeDaily::tradeDate).distinct().toList())
                .containsExactlyElementsOf(openDates);
    }

    @Test
    void investor_trade_window_without_the_requested_day_persists_that_day_as_incomplete() {
        LocalDate requested = LocalDate.of(2026, 10, 5);
        LocalDate previous = requested.minusDays(1);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", requested, requested)).willReturn(List.of(
                TradingCalendar.of("KOSPI", requested, true, null, null, "test")));
        given(prices.fetchInvestorTradeByStockDaily(stock, requested))
                .willReturn(List.of(investorRow(previous, "1", "1")));

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, requested, requested))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length).allSatisfy(row -> {
            assertThat(row.tradeDate()).isEqualTo(requested);
            assertThat(row.dataStatus()).isEqualTo("INCOMPLETE");
        });
    }

    @Test
    void absent_investor_response_is_persisted_as_incomplete_nulls_not_zero() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of());

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length).allSatisfy(value -> {
            assertThat(value.netQuantity()).isNull();
            assertThat(value.netAmount()).isNull();
            assertThat(value.dataStatus()).isEqualTo("INCOMPLETE");
        });
    }

    @Test
    void investor_response_with_another_business_date_marks_the_requested_date_incomplete() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "KIS:CTCA0903R")));
        given(prices.fetchInvestorTradeByStockDaily(stock, date)).willReturn(List.of(investorRow(
                date.minusDays(1), "1", "1", "1", "1", "1", "1", "1", "1", "1", "1",
                "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1")));

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");
        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).allSatisfy(row -> {
            assertThat(row.tradeDate()).isEqualTo(date);
            assertThat(row.dataStatus()).isEqualTo("INCOMPLETE");
        });
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

        assertThatThrownBy(() -> service.syncStockInvestorTrades(stock, date, date))
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
            assertThat(row.getRisingIssueCount()).isNull();
            assertThat(row.getFallingIssueCount()).isNull();
            assertThat(row.getUpperLimitIssueCount()).isNull();
            assertThat(row.getLowerLimitIssueCount()).isNull();
            assertThat(row.getSteadyIssueCount()).isNull();
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

        assertThatThrownBy(() -> service.syncIndexPrices(domesticSeries(), start, end))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

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
        given(calendar.findAll("NYSE", date, date)).willReturn(List.of(
                TradingCalendar.of("NYSE", date, true, null, null, "test")));
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
            assertThat(row.getRisingIssueCount()).isNull();
            assertThat(row.getFallingIssueCount()).isNull();
            assertThat(row.getUpperLimitIssueCount()).isNull();
            assertThat(row.getLowerLimitIssueCount()).isNull();
            assertThat(row.getSteadyIssueCount()).isNull();
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
        given(calendar.findAll("NYSE", date, date)).willReturn(List.of(
                TradingCalendar.of("NYSE", date, true, null, null, "test")));
        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", date, date)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20261005", "6750.25", "6730", "6760", "6700", " ", "N")));

        assertThatThrownBy(() -> service.syncIndexPrices(usSeries("SPX", "NYSE"), date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getVolume()).isNull();
            assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.INCOMPLETE);
        });
    }

    @Test
    void missing_us_open_day_is_persisted_as_incomplete_and_fails_the_collection() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        given(calendar.findAll("NYSE", date, date)).willReturn(List.of(
                TradingCalendar.of("NYSE", date, true, null, null, "test")));
        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", date, date)).willReturn(List.of());

        assertThatThrownBy(() -> service.syncIndexPrices(usSeries("SPX", "NYSE"), date, date))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete observations");

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(date);
            assertThat(row.getClosePrice()).isNull();
            assertThat(row.getPriceStatus())
                    .isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.INCOMPLETE);
        });
    }

    @Test
    void eod_session_end_uses_krx_local_close_and_new_york_dst_rules() {
        TradingCalendar krSession = TradingCalendar.of("KOSPI", LocalDate.of(2026, 11, 13), true,
                OffsetDateTime.parse("2026-11-13T09:00:00+09:00"),
                OffsetDateTime.parse("2026-11-13T15:30:00+09:00"), "test");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                krSession, Instant.parse("2026-11-13T06:29:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                krSession, Instant.parse("2026-11-13T06:30:00Z"))).isTrue();

        TradingCalendar usSummerSession = TradingCalendar.of("NASDAQ", LocalDate.of(2026, 7, 13), true,
                OffsetDateTime.parse("2026-07-13T09:30:00-04:00"),
                OffsetDateTime.parse("2026-07-13T16:00:00-04:00"), "test");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                usSummerSession, Instant.parse("2026-07-13T19:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                usSummerSession, Instant.parse("2026-07-13T20:00:00Z"))).isTrue();

        TradingCalendar usWinterSession = TradingCalendar.of("NYSE", LocalDate.of(2026, 11, 13), true,
                OffsetDateTime.parse("2026-11-13T09:30:00-05:00"),
                OffsetDateTime.parse("2026-11-13T16:00:00-05:00"), "test");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                usWinterSession, Instant.parse("2026-11-13T20:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                usWinterSession, Instant.parse("2026-11-13T21:00:00Z"))).isTrue();

        TradingCalendar closedDay = TradingCalendar.of("KOSPI", LocalDate.of(2026, 5, 1), false, null, null, "test");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                closedDay, Instant.parse("2026-11-26T00:00:00Z"))).isFalse();
    }

    @Test
    void krx_csat_session_is_not_complete_before_1630_kst_and_completes_at_1630_kst() {
        TradingCalendar csat = TradingCalendar.of("KOSPI", LocalDate.of(2026, 11, 19), true,
                OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                OffsetDateTime.parse("2026-11-19T16:30:00+09:00"), "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                csat, Instant.parse("2026-11-19T06:45:00Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                csat, Instant.parse("2026-11-19T07:29:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                csat, Instant.parse("2026-11-19T07:30:00Z"))).isTrue();
    }

    @Test
    void us_early_close_session_completes_at_1300_et_when_calendar_closes_at_is_present() {
        TradingCalendar early2026 = TradingCalendar.of("NYSE", LocalDate.of(2026, 11, 27), true,
                OffsetDateTime.parse("2026-11-27T09:30:00-05:00"),
                OffsetDateTime.parse("2026-11-27T13:00:00-05:00"), "NYSE:OFFICIAL:2026");
        TradingCalendar early2027 = TradingCalendar.of("NYSE", LocalDate.of(2027, 11, 26), true,
                OffsetDateTime.parse("2027-11-26T09:30:00-05:00"),
                OffsetDateTime.parse("2027-11-26T13:00:00-05:00"), "NYSE:OFFICIAL:2027");
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                early2026, Instant.parse("2026-11-27T17:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                early2026, Instant.parse("2026-11-27T18:00:00Z"))).isTrue();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                early2027, Instant.parse("2027-11-26T17:59:59Z"))).isFalse();
        assertThat(NewMarketDataSyncService.isEodSessionComplete(
                early2027, Instant.parse("2027-11-26T18:00:00Z"))).isTrue();
    }

    @Test
    void sync_index_prices_rejects_future_end_date_relative_to_injected_clock() {
        LocalDate futureDate = LocalDate.of(2026, 11, 20);
        NewMarketDataSyncService atNov19 = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        assertThatThrownBy(() -> atNov19.syncIndexPrices(domesticSeries(), futureDate, futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    private static JsonNode session(JsonNode fixture, String market, String date) {
        for (JsonNode day : fixture.path("markets").path(market).path("sessions")) {
            if (date.equals(day.path("date").asText())) return day;
        }
        throw new AssertionError("Missing session fixture: " + market + " " + date);
    }

    @Test
    void missing_open_day_index_response_is_persisted_as_incomplete_and_fails_the_collection() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        IndexSeries series = new IndexSeries(MarketIndex.of("0001", "코스피", "0"), "KIS",
                "kis.idxcode.KOSPI", "0001", "KOSPI", "PRICE", "NONE", "KRW", true);
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));
        given(indexPrices.fetchRawIndexDailyPrices("0001", date, date)).willReturn(List.of());

        assertThatThrownBy(() -> service.syncIndexPrices(series, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete");

        ArgumentCaptor<List<org.stockwellness.domain.stock.insight.IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getPriceStatus()).isEqualTo(org.stockwellness.domain.stock.insight.BenchmarkDataStatus.INCOMPLETE);
            assertThat(row.getClosePrice()).isNull();
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void domestic_index_sync_blocks_csat_same_day_before_1630_kst_and_succeeds_at_1630_kst() {
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        TradingCalendar csatSession = TradingCalendar.of("KOSPI", csatDate, true,
                OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                OffsetDateTime.parse("2026-11-19T16:30:00+09:00"),
                "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
        given(calendar.findAll("KOSPI", csatDate, csatDate)).willReturn(List.of(csatSession));

        NewMarketDataSyncService beforeCloseAt1545 = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> beforeCloseAt1545.syncIndexPrices(domesticSeries(), csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS index EOD session");

        NewMarketDataSyncService beforeCloseAt162959 = serviceAt(Instant.parse("2026-11-19T07:29:59Z"));
        assertThatThrownBy(() -> beforeCloseAt162959.syncIndexPrices(domesticSeries(), csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS index EOD session");

        verify(indexPrices, never()).fetchRawIndexDailyPrices(any(), any(), any());
        verify(indexDaily, never()).upsert(anyList());

        given(indexPrices.fetchRawIndexDailyPrices("0001", csatDate, csatDate)).willReturn(List.of(
                new KisDailySectorDetail("20261119", "2500.00", "1", "2", "0.04", "100", "250000",
                        "2490.00", "2510.00", "2480.00")));

        NewMarketDataSyncService atClose1630 = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        atClose1630.syncIndexPrices(domesticSeries(), csatDate, csatDate);

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(csatDate);
            assertThat(row.getPriceStatus()).isEqualTo(BenchmarkDataStatus.PROVISIONAL);
        });
    }

    @Test
    void overseas_index_sync_blocks_early_close_before_1300_et_and_succeeds_at_1300_et() {
        LocalDate earlyClose2026 = LocalDate.of(2026, 11, 27);
        TradingCalendar session2026 = TradingCalendar.of("NYSE", earlyClose2026, true,
                OffsetDateTime.parse("2026-11-27T09:30:00-05:00"),
                OffsetDateTime.parse("2026-11-27T13:00:00-05:00"),
                "NYSE:OFFICIAL:2026");
        given(calendar.findAll("NYSE", earlyClose2026, earlyClose2026)).willReturn(List.of(session2026));

        NewMarketDataSyncService beforeEarlyClose2026 = serviceAt(Instant.parse("2026-11-27T17:59:59Z"));
        assertThatThrownBy(() -> beforeEarlyClose2026.syncIndexPrices(usSeries("SPX", "NYSE"), earlyClose2026, earlyClose2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS overseas index EOD session");
        verify(indexPrices, never()).fetchOverseasIndexDailyPrices(any(), any(), any());

        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", earlyClose2026, earlyClose2026)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20261127", "6750.25", "6730", "6760", "6700", "0", "N")));
        NewMarketDataSyncService atEarlyClose2026 = serviceAt(Instant.parse("2026-11-27T18:00:00Z"));
        atEarlyClose2026.syncIndexPrices(usSeries("SPX", "NYSE"), earlyClose2026, earlyClose2026);
        verify(indexPrices).fetchOverseasIndexDailyPrices("SPX", earlyClose2026, earlyClose2026);

        LocalDate earlyClose2027 = LocalDate.of(2027, 11, 26);
        TradingCalendar session2027 = TradingCalendar.of("NYSE", earlyClose2027, true,
                OffsetDateTime.parse("2027-11-26T09:30:00-05:00"),
                OffsetDateTime.parse("2027-11-26T13:00:00-05:00"),
                "NYSE:OFFICIAL:2027");
        given(calendar.findAll("NYSE", earlyClose2027, earlyClose2027)).willReturn(List.of(session2027));
        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", earlyClose2027, earlyClose2027)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20271126", "6800.00", "6790", "6810", "6780", "0", "N")));

        NewMarketDataSyncService atEarlyClose2027 = serviceAt(Instant.parse("2027-11-26T18:00:00Z"));
        atEarlyClose2027.syncIndexPrices(usSeries("SPX", "NYSE"), earlyClose2027, earlyClose2027);
        verify(indexPrices).fetchOverseasIndexDailyPrices("SPX", earlyClose2027, earlyClose2027);
    }

    @Test
    void same_day_open_session_with_unknown_closes_at_fails_closed() {
        LocalDate date = LocalDate.of(2026, 11, 19);
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));

        NewMarketDataSyncService sameDayService = serviceAt(Instant.parse("2026-11-19T07:00:00Z"));
        assertThatThrownBy(() -> sameDayService.syncIndexPrices(domesticSeries(), date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("close time is unknown");

        verify(indexPrices, never()).fetchRawIndexDailyPrices(any(), any(), any());
        verify(indexDaily, never()).upsert(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void domestic_index_sync_fails_closed_when_range_includes_incomplete_same_day_csat_and_succeeds_on_retry() {
        LocalDate priorDate = LocalDate.of(2026, 11, 18);
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        given(calendar.findAll("KOSPI", priorDate, csatDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", priorDate, true,
                        OffsetDateTime.parse("2026-11-18T09:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-18T15:30:00+09:00"), "test"),
                TradingCalendar.of("KOSPI", csatDate, true,
                        OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-19T16:30:00+09:00"), "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE")));

        NewMarketDataSyncService beforeCsatClose = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> beforeCsatClose.syncIndexPrices(domesticSeries(), priorDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS index EOD session");

        verify(indexPrices, never()).fetchRawIndexDailyPrices(any(), any(), any());
        verify(indexDaily, never()).upsert(anyList());

        given(indexPrices.fetchRawIndexDailyPrices("0001", priorDate, csatDate)).willReturn(List.of(
                new KisDailySectorDetail("20261118", "2480.00", "1", "2", "0.04", "100", "250000",
                        "2470.00", "2490.00", "2460.00"),
                new KisDailySectorDetail("20261119", "2500.00", "1", "2", "0.04", "100", "250000",
                        "2490.00", "2510.00", "2480.00")));

        NewMarketDataSyncService afterCsatClose = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        afterCsatClose.syncIndexPrices(domesticSeries(), priorDate, csatDate);

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).extracting(row -> row.getId().getTradeDate())
                .containsExactly(priorDate, csatDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overseas_index_sync_fails_closed_when_range_includes_incomplete_early_close_and_succeeds_on_retry() {
        LocalDate priorDate = LocalDate.of(2026, 11, 25);
        LocalDate holiday = LocalDate.of(2026, 11, 26);
        LocalDate earlyClose = LocalDate.of(2026, 11, 27);
        given(calendar.findAll("NYSE", priorDate, earlyClose)).willReturn(List.of(
                TradingCalendar.of("NYSE", priorDate, true,
                        OffsetDateTime.parse("2026-11-25T09:30:00-05:00"),
                        OffsetDateTime.parse("2026-11-25T16:00:00-05:00"), "NYSE:OFFICIAL:2026"),
                TradingCalendar.of("NYSE", holiday, false, null, null, "NYSE:OFFICIAL:2026"),
                TradingCalendar.of("NYSE", earlyClose, true,
                        OffsetDateTime.parse("2026-11-27T09:30:00-05:00"),
                        OffsetDateTime.parse("2026-11-27T13:00:00-05:00"), "NYSE:OFFICIAL:2026")));

        NewMarketDataSyncService beforeEarlyClose = serviceAt(Instant.parse("2026-11-27T17:59:59Z"));
        assertThatThrownBy(() -> beforeEarlyClose.syncIndexPrices(usSeries("SPX", "NYSE"), priorDate, earlyClose))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS overseas index EOD session");

        verify(indexPrices, never()).fetchOverseasIndexDailyPrices(any(), any(), any());
        verify(indexDaily, never()).upsert(anyList());

        given(indexPrices.fetchOverseasIndexDailyPrices("SPX", priorDate, earlyClose)).willReturn(List.of(
                new KisOverseasIndexDailyPrice("20261125", "6740.00", "6730", "6750", "6720", "0", "N"),
                new KisOverseasIndexDailyPrice("20261127", "6750.25", "6730", "6760", "6700", "0", "N")));

        NewMarketDataSyncService atEarlyClose = serviceAt(Instant.parse("2026-11-27T18:00:00Z"));
        atEarlyClose.syncIndexPrices(usSeries("SPX", "NYSE"), priorDate, earlyClose);

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        assertThat(saved.getValue()).extracting(row -> row.getId().getTradeDate())
                .containsExactly(priorDate, earlyClose);
    }

    @Test
    void index_sync_skips_closed_holiday_range_without_provider_calls() {
        LocalDate holiday = LocalDate.of(2026, 5, 1);
        given(calendar.findAll("KOSPI", holiday, holiday)).willReturn(List.of(
                TradingCalendar.of("KOSPI", holiday, false, null, null, "test")));

        serviceAt(Instant.parse("2026-05-01T07:00:00Z")).syncIndexPrices(domesticSeries(), holiday, holiday);

        verify(indexPrices, never()).fetchRawIndexDailyPrices(any(), any(), any());
        verify(indexDaily, never()).upsert(anyList());
    }

    @Test
    void sync_trading_calendar_persists_regular_opening_day_csat_and_closed_krx_sessions() {
        LocalDate baseDate = LocalDate.of(2026, 10, 5);
        given(holidays.fetchHolidays(baseDate)).willReturn(List.of(
                new KisHolidayEntry("20250102", "Y"),
                new KisHolidayEntry("20251113", "Y"),
                new KisHolidayEntry("20260102", "Y"),
                new KisHolidayEntry("20260501", "N"),
                new KisHolidayEntry("20261005", "Y"),
                new KisHolidayEntry("20261119", "Y")));

        service.syncTradingCalendar(baseDate);

        ArgumentCaptor<List<TradingCalendar>> saved = ArgumentCaptor.forClass(List.class);
        verify(calendar).upsert(saved.capture());
        List<TradingCalendar> rows = saved.getValue();
        assertThat(rows).hasSize(12);

        for (String market : List.of("KOSPI", "KOSDAQ")) {
            assertThat(calendarRow(rows, market, LocalDate.of(2026, 10, 5))).satisfies(row -> {
                assertThat(row.isTradingDay()).isTrue();
                assertThat(row.getOpensAt()).isEqualTo(OffsetDateTime.parse("2026-10-05T09:00:00+09:00"));
                assertThat(row.getClosesAt()).isEqualTo(OffsetDateTime.parse("2026-10-05T15:30:00+09:00"));
                assertThat(row.getSource()).isEqualTo("KIS:CTCA0903R|KRX:REGULAR");
            });
            for (LocalDate openingDay : List.of(LocalDate.of(2025, 1, 2), LocalDate.of(2026, 1, 2))) {
                assertThat(calendarRow(rows, market, openingDay)).satisfies(row -> {
                    assertThat(row.isTradingDay()).isTrue();
                    assertThat(row.getOpensAt()).isEqualTo(openingDay.atTime(10, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
                    assertThat(row.getClosesAt()).isEqualTo(openingDay.atTime(15, 30).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
                    assertThat(row.getSource()).isEqualTo("KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
                });
            }
            for (LocalDate csatDay : List.of(LocalDate.of(2025, 11, 13), LocalDate.of(2026, 11, 19))) {
                assertThat(calendarRow(rows, market, csatDay)).satisfies(row -> {
                    assertThat(row.isTradingDay()).isTrue();
                    assertThat(row.getOpensAt()).isEqualTo(csatDay.atTime(10, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
                    assertThat(row.getClosesAt()).isEqualTo(csatDay.atTime(16, 30).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
                    assertThat(row.getSource()).isEqualTo("KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
                });
            }
            assertThat(calendarRow(rows, market, LocalDate.of(2026, 5, 1))).satisfies(row -> {
                assertThat(row.isTradingDay()).isFalse();
                assertThat(row.getOpensAt()).isNull();
                assertThat(row.getClosesAt()).isNull();
                assertThat(row.getSource()).isEqualTo("KIS:CTCA0903R|KRX:REGULAR");
            });
        }
    }

    @Test
    void sync_trading_calendar_fails_closed_for_unverified_open_years_and_invalid_responses() {
        LocalDate year2024 = LocalDate.of(2024, 12, 30);
        given(holidays.fetchHolidays(year2024)).willReturn(List.of(new KisHolidayEntry("20241230", "Y")));
        assertThatThrownBy(() -> service.syncTradingCalendar(year2024))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No verified KRX session hours");

        LocalDate year2027 = LocalDate.of(2027, 1, 4);
        given(holidays.fetchHolidays(year2027)).willReturn(List.of(new KisHolidayEntry("20270104", "Y")));
        assertThatThrownBy(() -> service.syncTradingCalendar(year2027))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No verified KRX session hours");

        LocalDate baseDate = LocalDate.of(2026, 10, 5);
        given(holidays.fetchHolidays(baseDate)).willReturn(List.of(
                new KisHolidayEntry("20261005", "Y"),
                new KisHolidayEntry("20261005", "Y")));
        assertThatThrownBy(() -> service.syncTradingCalendar(baseDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or duplicate");

        given(holidays.fetchHolidays(baseDate)).willReturn(List.of(new KisHolidayEntry("20261005", "X")));
        assertThatThrownBy(() -> service.syncTradingCalendar(baseDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or duplicate");

        given(holidays.fetchHolidays(baseDate)).willReturn(List.of(new KisHolidayEntry("20261006", "Y")));
        assertThatThrownBy(() -> service.syncTradingCalendar(baseDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("omitted the requested base date");

        given(holidays.fetchHolidays(LocalDate.of(2026, 12, 15))).willReturn(List.of(
                new KisHolidayEntry("20261215", "Y"),
                new KisHolidayEntry("20270104", "Y"),
                new KisHolidayEntry("20270104", "Y")));
        assertThatThrownBy(() -> service.syncTradingCalendar(LocalDate.of(2026, 12, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or duplicate");

        verify(calendar, never()).upsert(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_trading_calendar_skips_unverified_future_year_lookahead_rows_while_persisting_verified_base_date() {
        LocalDate baseDate = LocalDate.of(2026, 12, 15);
        given(holidays.fetchHolidays(baseDate)).willReturn(List.of(
                new KisHolidayEntry("20261215", "Y"),
                new KisHolidayEntry("20261225", "N"),
                new KisHolidayEntry("20261231", "N"),
                new KisHolidayEntry("20270101", "N"),
                new KisHolidayEntry("20270104", "Y")));

        service.syncTradingCalendar(baseDate);

        ArgumentCaptor<List<TradingCalendar>> saved = ArgumentCaptor.forClass(List.class);
        verify(calendar).upsert(saved.capture());
        List<TradingCalendar> rows = saved.getValue();
        assertThat(rows).hasSize(6);
        assertThat(rows).extracting(TradingCalendar::getTradeDate)
                .containsOnly(
                        LocalDate.of(2026, 12, 15),
                        LocalDate.of(2026, 12, 25),
                        LocalDate.of(2026, 12, 31))
                .doesNotContain(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 4));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_prices_uses_injected_clock_for_adjustment_as_of_on_future_date() {
        LocalDate tradeDate = LocalDate.of(2027, 6, 1);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", tradeDate, tradeDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", tradeDate, true,
                        OffsetDateTime.parse("2027-06-01T09:00:00+09:00"),
                        OffsetDateTime.parse("2027-06-01T15:30:00+09:00"), "test")));
        given(prices.fetchDailyPrices(stock, tradeDate, tradeDate)).willReturn(List.of(price(tradeDate, "100", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, tradeDate, tradeDate))
                .willReturn(List.of(price(tradeDate, "95", "N")));

        NewMarketDataSyncService futureClockService = serviceAt(Instant.parse("2027-06-01T07:30:00Z"));
        futureClockService.syncStockPrices(stock, tradeDate, tradeDate);

        ArgumentCaptor<List<StockPriceAdjustment>> adjustments = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(anyList(), adjustments.capture());
        assertThat(adjustments.getValue()).singleElement()
                .satisfies(adj -> assertThat(adj.getAdjustmentAsOf()).isEqualTo(tradeDate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_investor_trades_uses_injected_clock_for_collected_at() {
        LocalDate tradeDate = LocalDate.of(2026, 11, 19);
        Instant now = Instant.parse("2026-11-19T07:30:00Z");
        Stock stock = stock();
        given(calendar.findAll("KOSPI", tradeDate, tradeDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", tradeDate, true,
                        OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-19T16:30:00+09:00"), "test")));
        given(prices.fetchInvestorTradeByStockDaily(stock, tradeDate))
                .willReturn(List.of(investorRow(tradeDate, "1", "2")));

        serviceAt(now).syncStockInvestorTrades(stock, tradeDate, tradeDate);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length)
                .allSatisfy(row -> assertThat(row.collectedAt()).isEqualTo(now));
    }

    @Test
    void sync_stock_prices_and_investor_trades_reject_future_end_date_relative_to_injected_clock() {
        LocalDate futureDate = LocalDate.of(2026, 11, 20);
        Stock stock = stock();
        NewMarketDataSyncService atNov19 = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));

        assertThatThrownBy(() -> atNov19.syncStockPrices(stock, futureDate, futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
        assertThatThrownBy(() -> atNov19.syncStockInvestorTrades(stock, futureDate, futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");

        verify(calendar, never()).findAll(any(), any(), any());
        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_prices_blocks_csat_same_day_before_1630_kst_and_succeeds_at_1630_kst() {
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        Stock stock = stock();
        TradingCalendar csatSession = TradingCalendar.of("KOSPI", csatDate, true,
                OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                OffsetDateTime.parse("2026-11-19T16:30:00+09:00"),
                "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
        given(calendar.findAll("KOSPI", csatDate, csatDate)).willReturn(List.of(csatSession));

        NewMarketDataSyncService at1545 = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> at1545.syncStockPrices(stock, csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS stock price EOD session");

        NewMarketDataSyncService at162959 = serviceAt(Instant.parse("2026-11-19T07:29:59Z"));
        assertThatThrownBy(() -> at162959.syncStockPrices(stock, csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS stock price EOD session");

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchAdjustedDailyPrices(any(), any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());

        given(prices.fetchDailyPrices(stock, csatDate, csatDate)).willReturn(List.of(price(csatDate, "100", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, csatDate, csatDate)).willReturn(List.of(price(csatDate, "95", "N")));

        NewMarketDataSyncService at1630 = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        at1630.syncStockPrices(stock, csatDate, csatDate);

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(csatDate);
            assertThat(row.getDataStatus()).isEqualTo("PROVISIONAL");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_investor_trades_blocks_csat_same_day_before_1630_kst_and_succeeds_at_1630_kst() {
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        Stock stock = stock();
        TradingCalendar csatSession = TradingCalendar.of("KOSPI", csatDate, true,
                OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                OffsetDateTime.parse("2026-11-19T16:30:00+09:00"),
                "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE");
        given(calendar.findAll("KOSPI", csatDate, csatDate)).willReturn(List.of(csatSession));

        NewMarketDataSyncService at1545 = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> at1545.syncStockInvestorTrades(stock, csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS investor EOD session");

        NewMarketDataSyncService at162959 = serviceAt(Instant.parse("2026-11-19T07:29:59Z"));
        assertThatThrownBy(() -> at162959.syncStockInvestorTrades(stock, csatDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS investor EOD session");

        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(investorTrades, never()).upsert(anyList());

        given(prices.fetchInvestorTradeByStockDaily(stock, csatDate))
                .willReturn(List.of(investorRow(csatDate, "1", "2")));

        NewMarketDataSyncService at1630 = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        at1630.syncStockInvestorTrades(stock, csatDate, csatDate);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(saved.capture());
        assertThat(saved.getValue()).hasSize(InvestorSourceType.values().length)
                .allSatisfy(row -> {
                    assertThat(row.tradeDate()).isEqualTo(csatDate);
                    assertThat(row.dataStatus()).isEqualTo("PROVISIONAL");
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_prices_and_investor_trades_fail_closed_when_range_includes_incomplete_same_day_csat_and_succeed_on_retry() {
        LocalDate priorDate = LocalDate.of(2026, 11, 18);
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", priorDate, csatDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", priorDate, true,
                        OffsetDateTime.parse("2026-11-18T09:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-18T15:30:00+09:00"), "test"),
                TradingCalendar.of("KOSPI", csatDate, true,
                        OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-19T16:30:00+09:00"), "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE")));

        NewMarketDataSyncService beforeCsatClose = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> beforeCsatClose.syncStockPrices(stock, priorDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS stock price EOD session");
        assertThatThrownBy(() -> beforeCsatClose.syncStockInvestorTrades(stock, priorDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS investor EOD session");

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchAdjustedDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());

        given(prices.fetchDailyPrices(stock, priorDate, csatDate)).willReturn(List.of(
                price(priorDate, "100", "N"), price(csatDate, "101", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, priorDate, csatDate)).willReturn(List.of(
                price(priorDate, "95", "N"), price(csatDate, "96", "N")));
        given(prices.fetchInvestorTradeByStockDaily(stock, csatDate)).willReturn(List.of(
                investorRow(priorDate, "1", "2"), investorRow(csatDate, "3", "4")));

        NewMarketDataSyncService afterCsatClose = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        afterCsatClose.syncStockPrices(stock, priorDate, csatDate);
        afterCsatClose.syncStockInvestorTrades(stock, priorDate, csatDate);

        ArgumentCaptor<List<EodStockPrice>> savedPrices = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(savedPrices.capture(), anyList());
        assertThat(savedPrices.getValue()).extracting(row -> row.getId().getTradeDate())
                .containsExactly(priorDate, csatDate);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> savedInvestors = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(savedInvestors.capture());
        assertThat(savedInvestors.getValue().stream().map(EodStockInvestorTradeDaily::tradeDate).distinct().toList())
                .containsExactly(priorDate, csatDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_stock_prices_and_investor_trades_with_same_day_listing_fail_closed_before_close_and_succeed_on_retry() {
        LocalDate priorDate = LocalDate.of(2026, 11, 18);
        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "listingDate", csatDate);
        given(calendar.findAll("KOSPI", priorDate, csatDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", priorDate, true,
                        OffsetDateTime.parse("2026-11-18T09:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-18T15:30:00+09:00"), "test"),
                TradingCalendar.of("KOSPI", csatDate, true,
                        OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-19T16:30:00+09:00"), "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE")));

        NewMarketDataSyncService beforeCsatClose = serviceAt(Instant.parse("2026-11-19T06:45:00Z"));
        assertThatThrownBy(() -> beforeCsatClose.syncStockPrices(stock, priorDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS stock price EOD session");
        assertThatThrownBy(() -> beforeCsatClose.syncStockInvestorTrades(stock, priorDate, csatDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS investor EOD session");

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchAdjustedDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());

        given(prices.fetchDailyPrices(stock, csatDate, csatDate)).willReturn(List.of(price(csatDate, "101", "N")));
        given(prices.fetchAdjustedDailyPrices(stock, csatDate, csatDate)).willReturn(List.of(price(csatDate, "96", "N")));
        given(prices.fetchInvestorTradeByStockDaily(stock, csatDate)).willReturn(List.of(investorRow(csatDate, "3", "4")));

        NewMarketDataSyncService afterCsatClose = serviceAt(Instant.parse("2026-11-19T07:30:00Z"));
        afterCsatClose.syncStockPrices(stock, priorDate, csatDate);
        afterCsatClose.syncStockInvestorTrades(stock, priorDate, csatDate);

        ArgumentCaptor<List<EodStockPrice>> savedPrices = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(savedPrices.capture(), anyList());
        assertThat(savedPrices.getValue()).extracting(row -> row.getId().getTradeDate())
                .containsExactly(csatDate);

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> savedInvestors = ArgumentCaptor.forClass(List.class);
        verify(investorTrades).upsert(savedInvestors.capture());
        assertThat(savedInvestors.getValue().stream().map(EodStockInvestorTradeDaily::tradeDate).distinct().toList())
                .containsExactly(csatDate);
    }

    @Test
    void sync_stock_prices_and_investor_trades_fail_closed_when_same_day_open_session_lacks_closes_at() {
        LocalDate date = LocalDate.of(2026, 11, 19);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", date, date)).willReturn(List.of(
                TradingCalendar.of("KOSPI", date, true, null, null, "test")));

        NewMarketDataSyncService sameDayService = serviceAt(Instant.parse("2026-11-19T07:00:00Z"));
        assertThatThrownBy(() -> sameDayService.syncStockPrices(stock, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("close time is unknown");
        assertThatThrownBy(() -> sameDayService.syncStockInvestorTrades(stock, date, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("close time is unknown");

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());
    }

    @Test
    void sync_stock_prices_and_investor_trades_skip_empty_range_before_listing_date() {
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 6);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "listingDate", LocalDate.of(2026, 10, 10));
        given(calendar.findAll("KOSPI", start, end)).willReturn(List.of(
                TradingCalendar.of("KOSPI", start, true, null, null, "test"),
                TradingCalendar.of("KOSPI", end, true, null, null, "test")));

        NewMarketDataSyncService postClose = serviceAt(Instant.parse("2026-10-10T00:00:00Z"));
        postClose.syncStockPrices(stock, start, end);
        postClose.syncStockInvestorTrades(stock, start, end);

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchAdjustedDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());
    }

    @Test
    void sync_stock_prices_and_investor_trades_skip_closed_holiday_range() {
        LocalDate holiday = LocalDate.of(2026, 5, 1);
        Stock stock = stock();
        given(calendar.findAll("KOSPI", holiday, holiday)).willReturn(List.of(
                TradingCalendar.of("KOSPI", holiday, false, null, null, "test")));

        NewMarketDataSyncService atHoliday = serviceAt(Instant.parse("2026-05-01T07:00:00Z"));
        atHoliday.syncStockPrices(stock, holiday, holiday);
        atHoliday.syncStockInvestorTrades(stock, holiday, holiday);

        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(prices, never()).fetchAdjustedDailyPrices(any(), any(), any());
        verify(prices, never()).fetchInvestorTradeByStockDaily(any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());
        verify(investorTrades, never()).upsert(anyList());
    }

    private static TradingCalendar calendarRow(List<TradingCalendar> rows, String market, LocalDate date) {
        return rows.stream()
                .filter(row -> market.equals(row.getMarketCode()) && date.equals(row.getTradeDate()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @SuppressWarnings("unchecked")
    void stock_252_session_sync_splits_raw_and_adjusted_windows_within_100_day_cap() {
        List<TradingCalendar> syntheticCalendar = synthetic252SessionKrxCalendar(LocalDate.of(2025, 11, 17));
        LocalDate start = syntheticCalendar.getFirst().getTradeDate();
        LocalDate end = syntheticCalendar.getLast().getTradeDate();
        List<LocalDate> openDates = syntheticCalendar.stream()
                .filter(TradingCalendar::isTradingDay)
                .map(TradingCalendar::getTradeDate)
                .toList();
        assertThat(openDates).hasSize(252);

        Stock stock = stock();
        given(calendar.findAll("KOSPI", start, end)).willReturn(syntheticCalendar);
        given(prices.fetchDailyPrices(eq(stock), any(), any()))
                .willAnswer(invocation -> openDatesInWindow(openDates, invocation.getArgument(1), invocation.getArgument(2))
                        .stream().map(date -> price(date, "100", "N")).toList());
        given(prices.fetchAdjustedDailyPrices(eq(stock), any(), any()))
                .willAnswer(invocation -> openDatesInWindow(openDates, invocation.getArgument(1), invocation.getArgument(2))
                        .stream().map(date -> price(date, "95", "N")).toList());

        service.syncStockPrices(stock, start, end);

        ArgumentCaptor<LocalDate> rawFrom = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> rawTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(prices, times(4)).fetchDailyPrices(eq(stock), rawFrom.capture(), rawTo.capture());
        ArgumentCaptor<LocalDate> adjFrom = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> adjTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(prices, times(4)).fetchAdjustedDailyPrices(eq(stock), adjFrom.capture(), adjTo.capture());

        assertWindowRangesValid(rawFrom.getAllValues(), rawTo.getAllValues(), openDates, 100);
        assertThat(adjFrom.getAllValues()).containsExactlyElementsOf(rawFrom.getAllValues());
        assertThat(adjTo.getAllValues()).containsExactlyElementsOf(rawTo.getAllValues());

        ArgumentCaptor<List<EodStockPrice>> rawSaved = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<StockPriceAdjustment>> adjSaved = ArgumentCaptor.forClass(List.class);
        verify(eod, times(4)).upsertObservations(rawSaved.capture(), adjSaved.capture());

        List<EodStockPrice> allRaw = rawSaved.getAllValues().stream().flatMap(List::stream).toList();
        List<StockPriceAdjustment> allAdj = adjSaved.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(allRaw).hasSize(252);
        assertThat(allRaw.stream().map(row -> row.getId().getTradeDate()).toList()).containsExactlyElementsOf(openDates);
        assertThat(allRaw).allSatisfy(row -> {
            assertThat(row.getDataStatus()).isEqualTo("PROVISIONAL");
            assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:RAW");
        });
        assertThat(allAdj).hasSize(252);
        assertThat(allAdj.stream().map(row -> row.getId().getTradeDate()).toList()).containsExactlyElementsOf(openDates);
        assertThat(allAdj).allSatisfy(row -> assertThat(row.getSource()).isEqualTo("KIS:FHKST03010100:ADJUSTED"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void investor_252_session_sync_splits_into_nine_30_session_windows_and_filters_overlap() {
        List<TradingCalendar> syntheticCalendar = synthetic252SessionKrxCalendar(LocalDate.of(2025, 11, 17));
        LocalDate start = syntheticCalendar.getFirst().getTradeDate();
        LocalDate end = syntheticCalendar.getLast().getTradeDate();
        List<LocalDate> openDates = syntheticCalendar.stream()
                .filter(TradingCalendar::isTradingDay)
                .map(TradingCalendar::getTradeDate)
                .toList();
        assertThat(openDates).hasSize(252);

        Stock stock = stock();
        given(calendar.findAll("KOSPI", start, end)).willReturn(syntheticCalendar);
        given(prices.fetchInvestorTradeByStockDaily(eq(stock), any()))
                .willAnswer(invocation -> {
                    LocalDate asOf = invocation.getArgument(1);
                    int endIndex = openDates.indexOf(asOf);
                    int startIndex = Math.max(0, endIndex - 29);
                    return openDates.subList(startIndex, endIndex + 1).stream()
                            .map(date -> investorRow(date, "1", "2"))
                            .toList();
                });

        service.syncStockInvestorTrades(stock, start, end);

        ArgumentCaptor<LocalDate> asOfDates = ArgumentCaptor.forClass(LocalDate.class);
        verify(prices, times(9)).fetchInvestorTradeByStockDaily(eq(stock), asOfDates.capture());
        assertThat(asOfDates.getAllValues()).containsExactly(
                openDates.get(29), openDates.get(59), openDates.get(89), openDates.get(119),
                openDates.get(149), openDates.get(179), openDates.get(209), openDates.get(239),
                openDates.get(251));

        ArgumentCaptor<List<EodStockInvestorTradeDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(investorTrades, times(9)).upsert(saved.capture());
        int typesCount = InvestorSourceType.values().length;
        assertThat(typesCount).isEqualTo(13);
        assertThat(saved.getAllValues()).extracting(List::size).containsExactly(
                30 * typesCount, 30 * typesCount, 30 * typesCount, 30 * typesCount,
                30 * typesCount, 30 * typesCount, 30 * typesCount, 30 * typesCount,
                12 * typesCount);

        List<EodStockInvestorTradeDaily> allRows = saved.getAllValues().stream().flatMap(List::stream).toList();
        assertThat(allRows).hasSize(252 * 13);
        assertThat(allRows.stream().map(row -> row.tradeDate() + ":" + row.investorType()).distinct().count())
                .isEqualTo(252L * 13L);
        assertThat(allRows.stream().map(EodStockInvestorTradeDaily::tradeDate).distinct().toList())
                .containsExactlyElementsOf(openDates);
        assertThat(allRows).allSatisfy(row -> {
            assertThat(row.dataStatus()).isEqualTo("PROVISIONAL");
            assertThat(row.source()).isEqualTo("KIS:FHPTJ04160001:J");
            assertThat(row.netQuantity()).isNotNull();
            assertThat(row.netAmount()).isNotNull();
        });
        assertThat(allRows.stream().filter(row -> row.investorType() == InvestorSourceType.FOREIGN).toList())
                .hasSize(252)
                .allSatisfy(row -> assertThat(row.netAmount()).isEqualByComparingTo("2000000.00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void domestic_index_252_session_sync_splits_windows_within_50_day_cap() {
        List<TradingCalendar> syntheticCalendar = synthetic252SessionKrxCalendar(LocalDate.of(2025, 11, 17));
        LocalDate start = syntheticCalendar.getFirst().getTradeDate();
        LocalDate end = syntheticCalendar.getLast().getTradeDate();
        List<LocalDate> openDates = syntheticCalendar.stream()
                .filter(TradingCalendar::isTradingDay)
                .map(TradingCalendar::getTradeDate)
                .toList();
        assertThat(openDates).hasSize(252);

        given(calendar.findAll("KOSPI", start, end)).willReturn(syntheticCalendar);
        given(indexPrices.fetchRawIndexDailyPrices(eq("0001"), any(), any()))
                .willAnswer(invocation -> openDatesInWindow(openDates, invocation.getArgument(1), invocation.getArgument(2))
                        .stream()
                        .map(date -> new KisDailySectorDetail(
                                date.format(DateTimeFormatter.BASIC_ISO_DATE),
                                "2500.00", "1", "2", "0.04", "100", "250000", "2490.00", "2510.00", "2480.00"))
                        .toList());

        NewMarketDataSyncService afterEndClose = serviceAt(
                end.atTime(16, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant());
        afterEndClose.syncIndexPrices(domesticSeries(), start, end);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(indexPrices, times(8)).fetchRawIndexDailyPrices(eq("0001"),
                fromCaptor.capture(), toCaptor.capture());
        assertWindowRangesValid(fromCaptor.getAllValues(), toCaptor.getAllValues(), openDates, 50);

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        List<IndexDaily> rows = saved.getValue();
        assertThat(rows).hasSize(252);
        assertThat(rows.stream().map(row -> row.getId().getTradeDate()).toList()).containsExactlyElementsOf(openDates);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getPriceStatus()).isEqualTo(BenchmarkDataStatus.PROVISIONAL);
            assertThat(row.getPriceSourceApi()).isEqualTo("FHKUP03500100");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void overseas_index_252_session_sync_splits_windows_within_50_day_cap() {
        List<TradingCalendar> nyseCalendar = new UsMarketCalendar()
                .calendarDays("NYSE", LocalDate.of(2025, 11, 1), LocalDate.of(2026, 11, 13));
        List<LocalDate> allOpen = nyseCalendar.stream()
                .filter(TradingCalendar::isTradingDay)
                .map(TradingCalendar::getTradeDate)
                .toList();
        LocalDate start = allOpen.get(allOpen.size() - 252);
        LocalDate end = allOpen.getLast();
        List<TradingCalendar> windowCalendar = nyseCalendar.stream()
                .filter(day -> !day.getTradeDate().isBefore(start) && !day.getTradeDate().isAfter(end))
                .toList();
        List<LocalDate> openDates = windowCalendar.stream()
                .filter(TradingCalendar::isTradingDay)
                .map(TradingCalendar::getTradeDate)
                .toList();
        assertThat(openDates).hasSize(252);

        given(calendar.findAll("NYSE", start, end)).willReturn(windowCalendar);
        given(indexPrices.fetchOverseasIndexDailyPrices(eq("SPX"), any(), any()))
                .willAnswer(invocation -> openDatesInWindow(openDates, invocation.getArgument(1), invocation.getArgument(2))
                        .stream()
                        .map(date -> (BenchmarkPriceData)
                                new KisOverseasIndexDailyPrice(
                                        date.format(DateTimeFormatter.BASIC_ISO_DATE),
                                        "6750.25", "6730", "6760", "6700", "0", "N"))
                        .toList());

        NewMarketDataSyncService afterEndClose = serviceAt(
                end.atTime(16, 5).atZone(ZoneId.of("America/New_York")).toInstant());
        afterEndClose.syncIndexPrices(usSeries("SPX", "NYSE"), start, end);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(indexPrices, times(8)).fetchOverseasIndexDailyPrices(eq("SPX"),
                fromCaptor.capture(), toCaptor.capture());
        assertWindowRangesValid(fromCaptor.getAllValues(), toCaptor.getAllValues(), openDates, 50);

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily).upsert(saved.capture());
        List<IndexDaily> rows = saved.getValue();
        assertThat(rows).hasSize(252);
        assertThat(rows.stream().map(row -> row.getId().getTradeDate()).toList()).containsExactlyElementsOf(openDates);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getTradingAmount()).isNull();
            assertThat(row.getVolume()).isZero();
            assertThat(row.getPriceStatus()).isEqualTo(BenchmarkDataStatus.PROVISIONAL);
            assertThat(row.getPriceSourceApi()).isEqualTo("FHKST03030100");
        });
    }

    /** Builds a deterministic synthetic KRX calendar fixture (weekday open, weekend closed) for 252 open sessions. */
    private static List<TradingCalendar> synthetic252SessionKrxCalendar(LocalDate start) {
        List<TradingCalendar> rows = new ArrayList<>();
        int openCount = 0;
        for (LocalDate date = start; openCount < 252; date = date.plusDays(1)) {
            boolean open = date.getDayOfWeek().getValue() <= 5;
            if (open) {
                openCount++;
                rows.add(TradingCalendar.of("KOSPI", date, true,
                        date.atTime(9, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                        date.atTime(15, 30).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                        "SYNTHETIC:KRX_252_TEST"));
            } else {
                rows.add(TradingCalendar.of("KOSPI", date, false, null, null, "SYNTHETIC:KRX_252_TEST"));
            }
        }
        return rows;
    }

    private static List<LocalDate> openDatesInWindow(List<LocalDate> openDates, LocalDate from, LocalDate to) {
        return openDates.stream()
                .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                .toList();
    }

    private static void assertWindowRangesValid(List<LocalDate> froms, List<LocalDate> tos,
            List<LocalDate> expectedOpenDates, int maxCalendarDays) {
        assertThat(froms).isNotEmpty();
        assertThat(tos).hasSameSizeAs(froms);
        List<LocalDate> covered = new ArrayList<>();
        LocalDate previousTo = null;
        for (int i = 0; i < froms.size(); i++) {
            LocalDate from = froms.get(i);
            LocalDate to = tos.get(i);
            assertThat(from).isBeforeOrEqualTo(to);
            assertThat(ChronoUnit.DAYS.between(from, to) + 1).isLessThanOrEqualTo(maxCalendarDays);
            if (previousTo != null) {
                assertThat(from).isAfter(previousTo);
            }
            covered.addAll(openDatesInWindow(expectedOpenDates, from, to));
            previousTo = to;
        }
        assertThat(covered).containsExactlyElementsOf(expectedOpenDates);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overseas_index_captured_fixtures_with_rt_cd_zero_and_mod_yn_n_stay_provisional_and_keep_zero_volume()
            throws IOException {
        var resource = getClass().getResourceAsStream("/kis/source-contract/overseas-index-daily-2026-11-12.json");
        assertThat(resource).as("sanitized captured KIS overseas index fixture").isNotNull();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resource);
        JsonNode samples = root.path("samples");
        assertThat(samples).hasSize(3);

        LocalDate tradeDate = LocalDate.of(2026, 11, 12);
        UsMarketCalendar usCalendar = new UsMarketCalendar();
        given(calendar.findAll("NYSE", tradeDate, tradeDate))
                .willReturn(usCalendar.calendarDays("NYSE", tradeDate, tradeDate));
        given(calendar.findAll("NASDAQ", tradeDate, tradeDate))
                .willReturn(usCalendar.calendarDays("NASDAQ", tradeDate, tradeDate));

        // 2026-11-13 capture timestamp is after 2026-11-12 US 16:00 EST close (21:00 UTC).
        NewMarketDataSyncService postCloseService = serviceAt(Instant.parse("2026-11-13T00:00:00Z"));
        for (JsonNode sample : samples) {
            String code = sample.path("index_code").asText();
            assertThat(sample.path("rt_cd").asText()).isEqualTo("0");
            List<KisOverseasIndexDailyPrice> rows = mapper.convertValue(
                    sample.path("output2"),
                    new TypeReference<>() {});
            assertThat(rows).singleElement().satisfies(row -> assertThat(row.modYn()).isEqualTo("N"));

            String market = "NDX".equals(code) ? "NASDAQ" : "NYSE";
            given(indexPrices.fetchOverseasIndexDailyPrices(code, tradeDate, tradeDate))
                    .willReturn(rows.stream()
                            .map(BenchmarkPriceData.class::cast)
                            .toList());

            postCloseService.syncIndexPrices(usSeries(code, market), tradeDate, tradeDate);
        }

        ArgumentCaptor<List<IndexDaily>> saved = ArgumentCaptor.forClass(List.class);
        verify(indexDaily, times(3)).upsert(saved.capture());
        List<IndexDaily> persisted = saved.getAllValues().stream()
                .flatMap(List::stream)
                .toList();
        assertThat(persisted).hasSize(3).allSatisfy(row -> {
            assertThat(row.getId().getTradeDate()).isEqualTo(tradeDate);
            assertThat(row.getPriceStatus()).isEqualTo(BenchmarkDataStatus.PROVISIONAL);
            assertThat(row.getPriceSourceApi()).isEqualTo("FHKST03030100");
            assertThat(row.getTradingAmount()).isNull();
        });
        assertThat(persisted).anySatisfy(row -> {
            assertThat(row.getSeries().getProviderSeriesCode()).isEqualTo("SPX");
            assertThat(row.getClosePrice()).isEqualByComparingTo("7592.4000");
            assertThat(row.getVolume()).isZero();
        });
        assertThat(persisted).anySatisfy(row -> {
            assertThat(row.getSeries().getProviderSeriesCode()).isEqualTo("NDX");
            assertThat(row.getClosePrice()).isEqualByComparingTo("28289.1900");
            assertThat(row.getVolume()).isZero();
        });
        assertThat(persisted).anySatisfy(row -> {
            assertThat(row.getSeries().getProviderSeriesCode()).isEqualTo(".DJI");
            assertThat(row.getClosePrice()).isEqualByComparingTo("52297.0900");
            assertThat(row.getVolume()).isEqualTo(1015162860L);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void same_query_reread_close_change_with_mod_yn_n_stays_provisional() throws IOException {
        // 1) Stored sanitized real JSON fixture (2026-11-12 16:42 UTC, close 206500, mod_yn=N)
        var resource = getClass().getResourceAsStream("/kis/source-contract/domestic-daily-success.json");
        assertThat(resource).as("sanitized captured KIS domestic daily fixture").isNotNull();
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var fixture = mapper.readTree(resource);
        var response = fixture.path("response");
        assertThat(response.path("rt_cd").asText()).isEqualTo("0");
        KisDailyPriceDetail capturedRow = mapper.treeToValue(response.path("output2").get(0), KisDailyPriceDetail.class);
        assertThat(capturedRow.closePrice()).isEqualByComparingTo("206500");
        assertThat(capturedRow.modYn()).isEqualTo("N");

        // 2) Synthetic DTO reproducing the 2026-11-13 reread observation (close changed 206500 -> 208000, mod_yn=N;
        //    no separate 208000 JSON fixture file is stored in the repository)
        KisDailyPriceDetail syntheticReread208000 = new KisDailyPriceDetail(
                capturedRow.baseDate(),
                capturedRow.openPrice(),
                capturedRow.highPrice(),
                capturedRow.lowPrice(),
                new BigDecimal("208000"),
                capturedRow.volume(),
                capturedRow.transactionAmt(),
                capturedRow.flngClsCode(),
                capturedRow.prttRate(),
                "N",
                capturedRow.prdyVrssSign(),
                capturedRow.prdyVrss(),
                capturedRow.revlIssuReas());

        LocalDate tradeDate = capturedRow.baseDate();
        Stock stock = stock();
        given(calendar.findAll("KOSPI", tradeDate, tradeDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", tradeDate, true,
                        OffsetDateTime.parse("2026-11-12T09:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-12T15:30:00+09:00"),
                        "KIS:CTCA0903R|KRX:REGULAR")));
        given(prices.fetchDailyPrices(stock, tradeDate, tradeDate))
                .willReturn(List.of(capturedRow), List.of(syntheticReread208000));
        given(prices.fetchAdjustedDailyPrices(stock, tradeDate, tradeDate))
                .willReturn(List.of(), List.of());

        service.syncStockPrices(stock, tradeDate, tradeDate);
        service.syncStockPrices(stock, tradeDate, tradeDate);

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod, times(2)).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getAllValues().get(0)).singleElement().satisfies(first -> {
            assertThat(first.getClosePrice()).isEqualByComparingTo("206500");
            assertThat(first.getDataStatus()).isEqualTo("PROVISIONAL");
        });
        assertThat(saved.getAllValues().get(1)).singleElement().satisfies(reread -> {
            assertThat(reread.getClosePrice()).isEqualByComparingTo("208000");
            assertThat(reread.getDataStatus()).isEqualTo("PROVISIONAL");
        });
    }

    private NewMarketDataSyncService serviceAt(Instant instant) {
        return new NewMarketDataSyncService(prices, indexPrices, holidays, eod, investorTrades, indexDaily, calendar,
                Clock.fixed(instant, ZoneOffset.UTC));
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

    private static KisInvestorTradeByStockDaily investorRow(LocalDate date, String foreignQuantity, String foreignAmount) {
        return investorRow(date, foreignQuantity, foreignAmount,
                "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
                "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");
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
