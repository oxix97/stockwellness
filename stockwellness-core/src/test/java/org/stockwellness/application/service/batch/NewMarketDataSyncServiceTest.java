package org.stockwellness.application.service.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
            assertThat(row.getPreviousClosePrice()).isNull();
            assertThat(row.getNoTrade()).isFalse();
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
            assertThat(row.getPreviousClosePrice()).isNull();
            assertThat(row.getNoTrade()).isFalse();
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

    private static Stock stock() {
        return Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.empty(), StockStatus.ACTIVE);
    }

    private static KisDailyPriceDetail price(LocalDate date, String close, String modYn) {
        return new KisDailyPriceDetail(date, new BigDecimal("9.00"), new BigDecimal("11.00"),
                new BigDecimal("8.00"), new BigDecimal(close), 10L, new BigDecimal("1000.00"),
                null, null, modYn, null, null, null);
    }
}
