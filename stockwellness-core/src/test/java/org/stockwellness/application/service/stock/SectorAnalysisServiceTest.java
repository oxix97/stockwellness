package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SectorAnalysisServiceTest {

    private SectorAnalysisService sectorAnalysisService;

    @BeforeEach
    void setUp() {
        sectorAnalysisService = new SectorAnalysisService();
    }

    @Test
    @DisplayName("섹터 데이터 분석: 연속 매수 일수 및 기술적 지표가 올바르게 계산된다")
    void analyze_success() {
        // given
        MarketIndex index = MarketIndex.of("0001", "종합");
        LocalDate today = LocalDate.of(2026, 3, 2);
        
        SectorApiDto currentData = new SectorApiDto(
                "0001", "종합", today,
                new BigDecimal("2500.00"), new BigDecimal("1.5"),
                1000L, 500L
        );

        SectorInsight yesterday = SectorInsight.of(
                "종합", "0001", MarketType.KOSPI, today.minusDays(1),
                new BigDecimal("2450.00"), new BigDecimal("0.5"),
                800L, 200L,
                2, 1, // foreignConsecutive, instConsecutive
                TechnicalIndicators.empty(), false
        );

        List<BigDecimal> pastPrices = List.of(
                new BigDecimal("2400.00"), new BigDecimal("2410.00"), new BigDecimal("2420.00"),
                new BigDecimal("2430.00"), new BigDecimal("2440.00"), new BigDecimal("2450.00")
        );

        // when
        SectorInsight result = sectorAnalysisService.analyze(index, currentData, yesterday, pastPrices);

        // then
        assertThat(result.getSectorCode()).isEqualTo("0001");
        assertThat(result.getBaseDate()).isEqualTo(today);
        assertThat(result.getForeignConsecutiveBuyDays()).isEqualTo(3); // 2 + 1
        assertThat(result.getInstConsecutiveBuyDays()).isEqualTo(2); // 1 + 1
        assertThat(result.getTechnicalIndicators()).isNotNull();
        assertThat(result.getMarketType()).isEqualTo(MarketType.KOSPI);
    }

    @Test
    @DisplayName("수급이 끊기면 연속 매수 일수가 0으로 초기화된다")
    void analyze_consecutive_reset() {
        // given
        MarketIndex index = MarketIndex.of("0001", "종합");
        LocalDate today = LocalDate.of(2026, 3, 2);
        
        SectorApiDto currentData = new SectorApiDto(
                "0001", "종합", today,
                new BigDecimal("2500.00"), new BigDecimal("1.5"),
                -100L, 500L // Foreign net buy is negative
        );

        SectorInsight yesterday = SectorInsight.of(
                "종합", "0001", MarketType.KOSPI, today.minusDays(1),
                new BigDecimal("2450.00"), new BigDecimal("0.5"),
                800L, 200L,
                5, 5,
                TechnicalIndicators.empty(), false
        );

        // when
        SectorInsight result = sectorAnalysisService.analyze(index, currentData, yesterday, List.of());

        // then
        assertThat(result.getForeignConsecutiveBuyDays()).isZero();
        assertThat(result.getInstConsecutiveBuyDays()).isEqualTo(6);
    }
}
