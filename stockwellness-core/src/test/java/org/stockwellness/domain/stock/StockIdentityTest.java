package org.stockwellness.domain.stock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockIdentityTest {
    @Test
    void same_ticker_in_different_markets_has_distinct_market_scoped_identity() {
        var kospi = Stock.of("123456", null, "A", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        var kosdaq = Stock.of("123456", null, "B", MarketType.KOSDAQ, Currency.KRW, null, StockStatus.ACTIVE);

        assertThat(kospi.getTicker()).isEqualTo(kosdaq.getTicker());
        assertThat(kospi.getMarketCode()).isEqualTo("KOSPI");
        assertThat(kosdaq.getMarketCode()).isEqualTo("KOSDAQ");
        assertThat(kospi.getMarketCode()).isNotEqualTo(kosdaq.getMarketCode());
    }

    @Test
    void stock_requires_a_listed_market() {
        assertThatThrownBy(() -> Stock.of("0001", null, "코스피", MarketType.INDEX,
                Currency.KRW, null, StockStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("market");
    }
}
