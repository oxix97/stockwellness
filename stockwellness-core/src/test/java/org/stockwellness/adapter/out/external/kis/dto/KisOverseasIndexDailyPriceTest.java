package org.stockwellness.adapter.out.external.kis.dto;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KisOverseasIndexDailyPriceTest {

    @Test
    @DisplayName("KIS 실응답 해외 지수 샘플의 가격과 거래량을 그대로 매핑한다")
    void mapsObservedKisIndexRowsWithoutInventingValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream fixture = getClass().getResourceAsStream(
                "/kis/source-contract/overseas-index-daily-2026-11-12.json")) {
            JsonNode samples = objectMapper.readTree(fixture).path("samples");

            assertThat(samples).hasSize(3);
            assertThat(samples.findValuesAsText("index_code"))
                    .containsExactlyInAnyOrder("SPX", "NDX", ".DJI");
            assertThat(samples.findValuesAsText("rt_cd")).containsOnly("0");

            for (JsonNode sample : samples) {
                JsonNode sourceRow = sample.path("output2").get(0);
                List<KisOverseasIndexDailyPrice> rows = objectMapper.convertValue(sample.path("output2"),
                        new TypeReference<>() {});
                assertThat(rows).hasSize(1);

                KisOverseasIndexDailyPrice row = rows.getFirst();
                assertThat(row.baseDate()).isEqualTo(LocalDate.of(2026, 11, 12));
                assertThat(row.modYn()).isEqualTo("N");
                assertThat(row.openPrice()).isEqualByComparingTo(sourceRow.path("ovrs_nmix_oprc").asText());
                assertThat(row.highPrice()).isEqualByComparingTo(sourceRow.path("ovrs_nmix_hgpr").asText());
                assertThat(row.lowPrice()).isEqualByComparingTo(sourceRow.path("ovrs_nmix_lwpr").asText());
                assertThat(row.closePrice()).isEqualByComparingTo(sourceRow.path("ovrs_nmix_prpr").asText());
                assertThat(row.volume()).isEqualTo(Long.parseLong(sourceRow.path("acml_vol").asText()));
            }
        }
    }

    @Test
    @DisplayName("누락된 해외 지수 가격을 0으로 대체하지 않는다")
    void missingPricesRemainMissing() {
        BenchmarkPriceData data = new KisOverseasIndexDailyPrice(
                "20260401", null, " ", null, "", null, "0"
        );

        assertThat(data.closePrice()).isNull();
        assertThat(data.openPrice()).isNull();
        assertThat(data.highPrice()).isNull();
        assertThat(data.lowPrice()).isNull();
        assertThat(data.volume()).isNull();
    }

    @Test
    @DisplayName("해외 지수 일별 시세 DTO 매핑 및 BenchmarkPriceData 인터페이스 구현 검증")
    void mappingTest() {
        // given
        KisOverseasIndexDailyPrice dto = new KisOverseasIndexDailyPrice(
                "20260401", // stckBsopDate
                "5200.50",  // ovrsNmixPrpr (close)
                "5180.20",  // ovrsNmixOprc (open)
                "5210.80",  // ovrsNmixHgpr (high)
                "5175.10",  // ovrsNmixLwpr (low)
                "1234567",  // acmlVol
                "0"         // modYn
        );

        // when
        BenchmarkPriceData data = (BenchmarkPriceData) dto;

        // then
        assertThat(data.baseDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(data.closePrice()).isEqualByComparingTo("5200.50");
        assertThat(data.openPrice()).isEqualByComparingTo("5180.20");
        assertThat(data.highPrice()).isEqualByComparingTo("5210.80");
        assertThat(data.lowPrice()).isEqualByComparingTo("5175.10");
        assertThat(data.volume()).isEqualTo(1234567L);
        assertThat(data.prdyVrss()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(data.prdyCtrt()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
