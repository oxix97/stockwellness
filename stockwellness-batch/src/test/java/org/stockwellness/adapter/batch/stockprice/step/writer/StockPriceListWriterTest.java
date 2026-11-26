package org.stockwellness.adapter.batch.stockprice.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.application.port.out.external.kis.KisHolidayPort;
import org.stockwellness.application.port.out.external.kis.KisIndexPricePort;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.application.service.batch.NewMarketDataSyncService;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.price.EodStockPrice;

class StockPriceListWriterTest {

    @Test
    @SuppressWarnings("unchecked")
    void blocks_csat_same_day_write_before_1630_kst_and_persists_after_close() throws Exception {
        KisDailyPricePort prices = mock(KisDailyPricePort.class);
        KisIndexPricePort indexPrices = mock(KisIndexPricePort.class);
        KisHolidayPort holidays = mock(KisHolidayPort.class);
        EodStockPricePort eod = mock(EodStockPricePort.class);
        EodStockInvestorTradePort investorTrades = mock(EodStockInvestorTradePort.class);
        IndexDailyPort indexDaily = mock(IndexDailyPort.class);
        TradingCalendarPort calendar = mock(TradingCalendarPort.class);

        LocalDate csatDate = LocalDate.of(2026, 11, 19);
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.empty(), StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(calendar.findAll("KOSPI", csatDate, csatDate)).willReturn(List.of(
                TradingCalendar.of("KOSPI", csatDate, true,
                        OffsetDateTime.parse("2026-11-19T10:00:00+09:00"),
                        OffsetDateTime.parse("2026-11-19T16:30:00+09:00"),
                        "KIS:CTCA0903R|KRX:OFFICIAL_OVERRIDE")));

        var constructor = NewMarketDataSyncService.class.getDeclaredConstructor(
                KisDailyPricePort.class, KisIndexPricePort.class, KisHolidayPort.class,
                EodStockPricePort.class, EodStockInvestorTradePort.class, IndexDailyPort.class,
                TradingCalendarPort.class, Clock.class);
        constructor.setAccessible(true);

        NewMarketDataSyncService beforeClose = constructor.newInstance(
                prices, indexPrices, holidays, eod, investorTrades, indexDaily, calendar,
                Clock.fixed(Instant.parse("2026-11-19T06:45:00Z"), ZoneOffset.UTC));
        StockPriceListWriter intradayWriter = new StockPriceListWriter(beforeClose, csatDate);

        assertThatThrownBy(() -> intradayWriter.write(Chunk.of(List.of(stock))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No completed KIS stock price EOD session");
        verify(prices, never()).fetchDailyPrices(any(), any(), any());
        verify(eod, never()).upsertObservations(anyList(), anyList());

        given(prices.fetchDailyPrices(stock, csatDate, csatDate)).willReturn(List.of(
                new KisDailyPriceDetail(csatDate, new BigDecimal("70000"), new BigDecimal("71000"),
                        new BigDecimal("69000"), new BigDecimal("70500"), 100L, new BigDecimal("7050000"),
                        null, null, "N", null, null, null)));
        given(prices.fetchAdjustedDailyPrices(stock, csatDate, csatDate)).willReturn(List.of());

        NewMarketDataSyncService afterClose = constructor.newInstance(
                prices, indexPrices, holidays, eod, investorTrades, indexDaily, calendar,
                Clock.fixed(Instant.parse("2026-11-19T07:30:00Z"), ZoneOffset.UTC));
        StockPriceListWriter postCloseWriter = new StockPriceListWriter(afterClose, csatDate);

        postCloseWriter.write(Chunk.of(List.of(stock)));

        ArgumentCaptor<List<EodStockPrice>> saved = ArgumentCaptor.forClass(List.class);
        verify(eod).upsertObservations(saved.capture(), anyList());
        assertThat(saved.getValue()).singleElement()
                .satisfies(row -> assertThat(row.getId().getTradeDate()).isEqualTo(csatDate));
    }
}
