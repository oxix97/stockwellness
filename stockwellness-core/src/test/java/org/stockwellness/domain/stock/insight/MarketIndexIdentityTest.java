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

    @Test
    void classifies_all_four_supported_domestic_benchmarks_as_market_indices() {
        assertThat(java.util.List.of(
                MarketIndex.of("0001", "KOSPI", "0"),
                MarketIndex.of("1001", "KOSDAQ", "1"),
                MarketIndex.of("2001", "KOSPI 200", "0"),
                MarketIndex.of("3003", "KOSDAQ 150", "1")))
                .extracting(MarketIndex::getIndexKind)
                .containsOnly(MarketIndexKind.MARKET);

        assertThat(MarketIndex.of("0027", "제약", "0").getIndexKind())
                .isEqualTo(MarketIndexKind.SECTOR);
    }

    @Test
    void keepsLogicalIdentityMarketScopedWhilePublicBenchmarkCodesRemainExplicit() {
        MarketIndex kospi = MarketIndex.of("0099", "KOSPI 업종", "0");
        MarketIndex kosdaq = MarketIndex.of("0099", "KOSDAQ 업종", "1");
        MarketIndex publicBenchmark = MarketIndex.of("0001", "종합", "0");

        assertThat(kospi.getIndexKey()).isNotEqualTo(kosdaq.getIndexKey());
        assertThat(kospi.getProviderCode()).isEqualTo(kosdaq.getProviderCode());
        assertThat(publicBenchmark.getPublicBenchmarkCode()).isEqualTo("0001");
        assertThat(kospi.getPublicBenchmarkCode()).isNull();
    }
}
