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
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorTradeByStockDaily;
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
                otherOrganizationQuantity, otherOrganizationAmount, otherCorporateQuantity,
                otherCorporateAmount, otherQuantity, otherAmount);
    }

    private static KisDailyPriceDetail price(LocalDate date, String close, String modYn) {
        BigDecimal closePrice = new BigDecimal(close);
        return new KisDailyPriceDetail(date, closePrice, closePrice.add(BigDecimal.ONE),
                closePrice.subtract(BigDecimal.ONE).max(BigDecimal.ZERO), closePrice, 10L, new BigDecimal("1000.00"),
                null, null, modYn, null, null, null);
    }
}
