package org.stockwellness.domain.stock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.insight.MarketIndex;
import static org.assertj.core.api.Assertions.assertThat;

class StockSectorTest {

    @Test
    @DisplayName("소분류 코드가 존재하면 소분류 코드를 sectorCode로 결정하고 명칭도 소분류 기준이다")
    void resolveBySmallCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 대형"));
        indexMap.put("0012", new MarketIndex("0012", "전기전자"));
        indexMap.put("1234", new MarketIndex("1234", "반도체세부"));

        // when (Large: 0001, Medium: 0012, Small: 1234)
        StockSector sector = StockSector.of("0001", "0012", "1234", indexMap);

        // then
        assertThat(sector.getSectorCode()).isEqualTo("1234");
        assertThat(sector.getSectorName()).isEqualTo("반도체세부");
    }

    @Test
    @DisplayName("소분류가 0000이면 중분류 코드를 sectorCode로 결정한다")
    void resolveByMediumCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 대형"));
        indexMap.put("0012", new MarketIndex("0012", "전기전자"));

        // when (Small: 0000)
        StockSector sector = StockSector.of("0001", "0012", "0000", indexMap);

        // then
        assertThat(sector.getSectorCode()).isEqualTo("0012");
        assertThat(sector.getSectorName()).isEqualTo("전기전자");
    }

    @Test
    @DisplayName("소/중분류 모두 0000이면 대분류 코드를 sectorCode로 결정한다")
    void resolveByLargeCode() {
        // given
        Map<String, MarketIndex> indexMap = new HashMap<>();
        indexMap.put("0001", new MarketIndex("0001", "코스피 종합"));

        // when
        StockSector sector = StockSector.of("0001", "0000", "0000", indexMap);

        // then
        assertThat(sector.getSectorCode()).isEqualTo("0001");
        assertThat(sector.getSectorName()).isEqualTo("코스피 종합");
    }

    @Test
    @DisplayName("모든 코드가 매핑되지 않아도 sectorCode는 우선순위에 따라 결정된다")
    void resolveDefault() {
        // given
        Map<String, MarketIndex> emptyMap = new HashMap<>();

        // when & then
        StockSector sector1 = StockSector.of("0001", "9999", "0000", emptyMap);
        assertThat(sector1.getSectorCode()).isEqualTo("9999");
        assertThat(sector1.getSectorName()).isEqualTo("0001");

        StockSector sector2 = StockSector.of(null, null, null, emptyMap);
        assertThat(sector2.getSectorCode()).isNull();
        assertThat(sector2.getSectorName()).isEqualTo("미분류");

        StockSector sector3 = StockSector.of("", " ", "0000", emptyMap);
        assertThat(sector3.getSectorCode()).isEqualTo(""); 
        assertThat(sector3.getSectorName()).isEqualTo("미분류");
    }
}
