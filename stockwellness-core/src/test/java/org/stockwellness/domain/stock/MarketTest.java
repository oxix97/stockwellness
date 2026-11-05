package org.stockwellness.domain.stock;

import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.exception.GlobalException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketTest {
    @Test
    void stores_market_identity_and_calendar_context() {
        var market = Market.of("KOSPI", "유가증권시장", "KR", "KRW", "Asia/Seoul");

        assertThat(market.getCode()).isEqualTo("KOSPI");
        assertThat(market.getCountryCode()).isEqualTo("KR");
        assertThat(market.getCurrency()).isEqualTo("KRW");
        assertThat(market.getTimezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void rejects_invalid_market_code_currency_country_or_timezone() {
        assertThatThrownBy(() -> Market.of("", "KOSPI", "KR", "KRW", "Asia/Seoul"))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> Market.of("KOSPI", "KOSPI", "KOR", "KRW", "Asia/Seoul"))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> Market.of("KOSPI", "KOSPI", "KR", "KR", "Asia/Seoul"))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> Market.of("KOSPI", "KOSPI", "KR", "KRW", "unknown/zone"))
                .isInstanceOf(GlobalException.class);
    }
}
