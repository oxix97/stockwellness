package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectorInsightService 단위 테스트")
class SectorInsightServiceTest {

    @InjectMocks
    private SectorInsightService sectorInsightService;

    @Mock
    private SectorInsightPort sectorInsightPort;

    private SectorInsight mockInsight(String sectorCode, LocalDate date) {
        SectorIndicators indicators = SectorIndicators.of(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1.5),
                100L, -50L, 0, 0
        );
        return SectorInsight.of("전기전자", sectorCode, MarketType.KOSPI, date, indicators, null, false);
    }

    @Nested
    @DisplayName("getSectorDetail")
    class GetSectorDetail {

        @Test
        @DisplayName("당일 데이터가 있으면 해당 데이터를 반환한다")
        void returnsTodayData_whenExists() {
            LocalDate today = LocalDate.now();
            SectorInsight insight = mockInsight("0007", today);
            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.of(insight));

            SectorDetailResult result = sectorInsightService.getSectorDetail("0007", today);

            assertThat(result.sectorCode()).isEqualTo("0007");
        }

        @Test
        @DisplayName("당일 데이터 없을 때 findLatestBefore 결과를 반환한다")
        void fallsBackToLatestBefore_whenTodayDataAbsent() {
            LocalDate today = LocalDate.now();
            SectorInsight yesterday = mockInsight("0007", today.minusDays(1));
            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.empty());
            given(sectorInsightPort.findLatestBefore("0007", today)).willReturn(Optional.of(yesterday));

            SectorDetailResult result = sectorInsightService.getSectorDetail("0007", today);

            assertThat(result.sectorCode()).isEqualTo("0007");
            verify(sectorInsightPort).findLatestBefore("0007", today);
        }

        @Test
        @DisplayName("당일 데이터도 없고 최근 데이터도 없으면 예외를 던진다")
        void throwsException_whenNoDataExists() {
            LocalDate today = LocalDate.now();
            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.empty());
            given(sectorInsightPort.findLatestBefore("0007", today)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sectorInsightService.getSectorDetail("0007", today))
                    .isInstanceOf(SectorDomainException.class);
        }
    }

    @Nested
    @DisplayName("compareWithMarket")
    class CompareWithMarket {

        @Test
        @DisplayName("당일 데이터 없을 때 findLatestBefore 기준 날짜로 시장 비교를 수행한다")
        void fallsBackToLatestBefore_andUsesEffectiveDate() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            SectorInsight sector = mockInsight("0007", yesterday);
            SectorInsight market = mockInsight("0001", yesterday);

            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.empty());
            given(sectorInsightPort.findLatestBefore("0007", today)).willReturn(Optional.of(sector));
            given(sectorInsightPort.findByCodesAndDate(List.of("0007", "0001"), yesterday))
                    .willReturn(List.of(sector, market));
            given(sectorInsightPort.findHistoryByCode(eq("0007"), eq(yesterday), any(int.class)))
                    .willReturn(List.of());
            given(sectorInsightPort.findHistoryByCode(eq("0001"), eq(yesterday), any(int.class)))
                    .willReturn(List.of());

            SectorComparisonResult result = sectorInsightService.compareWithMarket("0007", today);

            assertThat(result.sectorCode()).isEqualTo("0007");
            verify(sectorInsightPort).findLatestBefore("0007", today);
            verify(sectorInsightPort).findByCodesAndDate(List.of("0007", "0001"), yesterday);
        }

        @Test
        @DisplayName("RS가 0.2보다 크면 OUTPERFORM을 반환한다")
        void returnsOutperform_whenRSAbovePointTwo() {
            LocalDate today = LocalDate.now();
            SectorIndicators sectorInd = SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.0), 0L, 0L, 0, 0); // +1.0%
            SectorInsight sector = SectorInsight.of("전기전자", "0007", MarketType.KOSPI, today, sectorInd, null, false);
            
            SectorIndicators marketInd = SectorIndicators.of(BigDecimal.valueOf(2500), BigDecimal.valueOf(0.7), 0L, 0L, 0, 0); // +0.7%
            SectorInsight market = SectorInsight.of("코스피", "0001", MarketType.KOSPI, today, marketInd, null, false);
            // RS = 0.3 (> 0.2)

            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.of(sector));
            given(sectorInsightPort.findByCodesAndDate(List.of("0007", "0001"), today))
                    .willReturn(List.of(sector, market));

            SectorComparisonResult result = sectorInsightService.compareWithMarket("0007", today);

            assertThat(result.performanceStatus()).isEqualTo("OUTPERFORM");
        }

        @Test
        @DisplayName("당일 데이터도 없고 최근 데이터도 없으면 예외를 던진다")
        void throwsException_whenNoDataExists() {
            LocalDate today = LocalDate.now();
            given(sectorInsightPort.findBySectorCodeAndDate("0007", today)).willReturn(Optional.empty());
            given(sectorInsightPort.findLatestBefore("0007", today)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sectorInsightService.compareWithMarket("0007", today))
                    .isInstanceOf(SectorDomainException.class);
        }
    }
}
