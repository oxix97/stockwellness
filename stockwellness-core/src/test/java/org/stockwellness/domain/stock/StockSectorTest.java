package org.stockwellness.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockSectorTest {

    @Test
    @DisplayName("소분류 코드가 존재하면 소분류 명칭을 업종명으로 결정한다")
    void resolveBySmallCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 대형"));
        indexMap.put("0012", new MarketIndex("0012", "전기전자"));
        indexMap.put("1234", new MarketIndex("1234", "반도체세부"));

        // when (Large: 0001, Medium: 0012, Small: 1234)
        StockSector sector = StockSector.of("0001", "0012", "1234", indexMap);

        // then
        assertThat(sector.getSectorName()).isEqualTo("반도체세부");
    }

    @Test
    @DisplayName("소분류가 없거나 매핑되지 않으면 중분류 명칭을 업종명으로 결정한다")
    void resolveByMediumCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 대형"));
        indexMap.put("0012", new MarketIndex("0012", "전기전자"));

        // when (Small: 0000 혹은 없는 코드)
        StockSector sector = StockSector.of("0001", "0012", "0000", indexMap);

        // then
        assertThat(sector.getSectorName()).isEqualTo("전기전자");
    }

    @Test
    @DisplayName("소/중분류 모두 매핑되지 않으면 대분류 명칭을 사용한다")
    void resolveByLargeCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 종합"));

        // when
        StockSector sector = StockSector.of("0001", "9999", "0000", indexMap);

        // then
        assertThat(sector.getSectorName()).isEqualTo("코스피 종합");
    }

    @Test
    @DisplayName("모든 코드가 매핑되지 않으면 대분류 코드 자체를 사용하고, 대분류도 없으면 미분류로 처리한다")
    void resolveDefault() {
        // given
        Map<String, MarketIndex> emptyMap = new HashMap<>();

        // when & then
        assertThat(StockSector.of("0001", "9999", "0000", emptyMap).getSectorName()).isEqualTo("0001");
        assertThat(StockSector.of(null, null, null, emptyMap).getSectorName()).isEqualTo("미분류");
        assertThat(StockSector.of("", " ", "0000", emptyMap).getSectorName()).isEqualTo("미분류");
    }
}
