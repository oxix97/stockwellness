package org.stockwellness.domain.stock.insight;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketIndexIdentityTest {
    @Test
    void mapsOnlyVerifiedProviderDivisionsAndLeavesUnknownDivisionUnclassified() {
        MarketIndex kospi = MarketIndex.of("0001", "종합", "0");
        MarketIndex kosdaq = MarketIndex.of("1001", "KOSDAQ", "1");
        MarketIndex unknown = MarketIndex.of("2030", "선물", "2");

        assertThat(kospi.getMarketCode()).isEqualTo("KOSPI");
        assertThat(kosdaq.getMarketCode()).isEqualTo("KOSDAQ");
        assertThat(unknown.getMarketCode()).isNull();
        assertThat(unknown.getIndexKind()).isEqualTo(MarketIndexKind.OTHER);
    }
}
