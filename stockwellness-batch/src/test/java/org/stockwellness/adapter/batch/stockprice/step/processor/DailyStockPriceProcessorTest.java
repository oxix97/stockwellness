package org.stockwellness.adapter.batch.stockprice.step.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.global.util.DateUtil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyStockPriceProcessorTest {

    private final KisDailyPricePort kisDailyPricePort = mock(KisDailyPricePort.class);

    @Test
    void usesMultiPriceOnlyForToday() {
        Stock stock = StockFixture.createSamsung();
        LocalDate today = DateUtil.today();
        when(kisDailyPricePort.fetchMultiStockPrices(List.of(stock.getTicker())))
                .thenReturn(List.of(multiPrice(stock.getTicker())));

        List<StockPrice> result = new DailyStockPriceProcessor(kisDailyPricePort, today).process(List.of(stock));

        assertThat(result).singleElement().satisfies(price -> {
            assertThat(price.getId().getBaseDate()).isEqualTo(today);
            assertThat(price.getClosePrice()).isEqualByComparingTo("70500");
        });
        verify(kisDailyPricePort).fetchMultiStockPrices(List.of(stock.getTicker()));
        verify(kisDailyPricePort, never()).fetchDailyPrices(any(), any(), any());
    }

    @Test
    void usesDateMatchedDailyPricesForHistoricalTargetDate() {
        Stock stock = StockFixture.createSamsung();
        LocalDate targetDate = DateUtil.today().minusDays(1);
        LocalDate previousTradingDate = targetDate.minusDays(1);
        when(kisDailyPricePort.fetchDailyPrices(stock, targetDate.minusDays(14), targetDate))
                .thenReturn(List.of(
                        dailyPrice(targetDate, "71", "73", "70", "72"),
                        dailyPrice(previousTradingDate, "69", "71", "68", "70")
                ));

        List<StockPrice> result = new DailyStockPriceProcessor(kisDailyPricePort, targetDate).process(List.of(stock));

        assertThat(result).singleElement().satisfies(price -> {
            assertThat(price.getId().getBaseDate()).isEqualTo(targetDate);
            assertThat(price.getClosePrice()).isEqualByComparingTo("72");
            assertThat(price.getPreviousClosePrice()).isEqualByComparingTo("70");
        });
        verify(kisDailyPricePort, never()).fetchMultiStockPrices(anyList());
    }

    @Test
    void failsWhenHistoricalResponseDoesNotContainTargetDate() {
        Stock stock = StockFixture.createSamsung();
        LocalDate targetDate = DateUtil.today().minusDays(1);
        when(kisDailyPricePort.fetchDailyPrices(stock, targetDate.minusDays(14), targetDate)).thenReturn(List.of());

        assertThatThrownBy(() -> new DailyStockPriceProcessor(kisDailyPricePort, targetDate).process(List.of(stock)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("005930")
                .hasMessageContaining(targetDate.toString());
    }

    @Test
    void failsWhenCurrentMultiPriceResponseOmitsARequestedTicker() {
        Stock samsung = StockFixture.createSamsung();
        Stock kodex = StockFixture.createKodex200();
        when(kisDailyPricePort.fetchMultiStockPrices(List.of(samsung.getTicker(), kodex.getTicker())))
                .thenReturn(List.of(multiPrice(samsung.getTicker())));

        assertThatThrownBy(() -> new DailyStockPriceProcessor(kisDailyPricePort, DateUtil.today())
                .process(List.of(samsung, kodex)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(kodex.getTicker());
    }

    @Test
    void rejectsFutureTargetDateWithoutCallingKis() {
        LocalDate futureDate = DateUtil.today().plusDays(1);

        assertThatThrownBy(() -> new DailyStockPriceProcessor(kisDailyPricePort, futureDate)
                .process(List.of(StockFixture.createSamsung())))
                .isInstanceOf(IllegalArgumentException.class);

        verify(kisDailyPricePort, never()).fetchMultiStockPrices(anyList());
        verify(kisDailyPricePort, never()).fetchDailyPrices(any(), any(), any());
    }

    private static KisMultiStockPriceDetail multiPrice(String ticker) {
        return new KisMultiStockPriceDetail(ticker, "name", new BigDecimal("70500"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("70000"), new BigDecimal("71000"), new BigDecimal("69000"),
                1000L, new BigDecimal("70500000"), new BigDecimal("70000"), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static KisDailyPriceDetail dailyPrice(
            LocalDate date, String open, String high, String low, String close
    ) {
        return new KisDailyPriceDetail(date, new BigDecimal(open), new BigDecimal(high), new BigDecimal(low),
                new BigDecimal(close), 1000L, new BigDecimal("70500000"), null, null, "N", null, null, null);
    }
}
