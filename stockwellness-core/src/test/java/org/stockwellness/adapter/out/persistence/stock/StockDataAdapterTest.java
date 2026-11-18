package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockDataAdapterTest {

    @Mock private StockPriceReadPort stockPriceReadPort;
    @InjectMocks private StockDataAdapter stockDataAdapter;

    @Test
    void watchlistPricesUseStockIdAndLeaveMissingChangeNull() {
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockPriceReadPort.recent(Set.of(1L), "KRX", LocalDate.now(), 2, PriceBasis.RAW))
                .willReturn(Map.of(1L, List.of(observation(LocalDate.now(), "80000"))));

        var result = stockDataAdapter.getStockDetails(List.of(stock));

        assertThat(result).containsKey(1L);
        assertThat(result.get(1L).currentPrice()).isEqualByComparingTo("80000");
        assertThat(result.get(1L).fluctuationRate()).isNull();
        assertThat(result.get(1L).rsi()).isNull();
    }

    @Test
    void stocksWithoutFinalPriceAreOmittedForWatchlistFallback() {
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockPriceReadPort.recent(Set.of(1L), "KRX", LocalDate.now(), 2, PriceBasis.RAW))
                .willReturn(Map.of(1L, List.of()));

        assertThat(stockDataAdapter.getStockDetails(List.of(stock))).isEmpty();
    }

    private static PriceObservation observation(LocalDate date, String close) {
        BigDecimal value = new BigDecimal(close);
        return new PriceObservation(1L, date, "KRX", PriceBasis.RAW, value, value, value, value,
                100L, BigDecimal.TEN, "FINAL", 1);
    }
}
