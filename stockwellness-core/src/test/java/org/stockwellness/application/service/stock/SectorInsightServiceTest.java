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

import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectorInsightService 단위 테스트")
class SectorInsightServiceTest {

    @InjectMocks
    private SectorInsightService sectorInsightService;

    @Mock
    private SectorInsightPort sectorInsightPort;

    @Mock
    private BenchmarkPricePort benchmarkPricePort;

    private SectorInsight mockInsight(String sectorCode, LocalDate date) {
        SectorIndicators indicators = SectorIndicators.of(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1.5),
                100L, -50L, 0, 0
        );
        return SectorInsight.of("전기전자", sectorCode, MarketType.KOSPI, date, indicators, null, false);
    }

    private BenchmarkPrice mockBenchmark(String ticker, LocalDate date, BigDecimal changeRate) {
        BenchmarkPrice bp = BenchmarkPrice.of("Benchmark", ticker, date, BigDecimal.valueOf(2500));
        bp.updatePrices(null, null, null, bp.getClosePrice(), changeRate, 0L);
        return bp;
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
        @DisplayName("최신 데이터를 기준으로 시장 비교를 수행한다")
        void usesLatestDataForComparison() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            SectorInsight sector = mockInsight("0007", yesterday);
            BenchmarkPrice market = mockBenchmark("0001", yesterday, BigDecimal.valueOf(0.5));

            given(sectorInsightPort.findLatestBefore(eq("0007"), any(LocalDate.class))).willReturn(Optional.of(sector));
            given(benchmarkPricePort.findByTickerAndBaseDate("0001", yesterday)).willReturn(Optional.of(market));
            given(sectorInsightPort.findHistoryByCode(eq("0007"), eq(yesterday), any(int.class)))
                    .willReturn(List.of());
            given(benchmarkPricePort.findHistoryByTicker(eq("0001"), eq(yesterday), any(int.class)))
                    .willReturn(List.of());

            SectorComparisonResult result = sectorInsightService.compareWithMarket("0007");

            assertThat(result.sectorCode()).isEqualTo("0007");
            verify(sectorInsightPort).findLatestBefore(eq("0007"), any(LocalDate.class));
            verify(benchmarkPricePort).findByTickerAndBaseDate("0001", yesterday);
        }

        @Test
        @DisplayName("RS가 0.4보다 크면 OUTPERFORM을 반환한다")
        void returnsOutperform_whenRSAbovePointFour() {
            LocalDate today = LocalDate.now();
            SectorIndicators sectorInd = SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.0), 0L, 0L, 0, 0);
            SectorInsight sector = SectorInsight.of("전기전자", "0007", MarketType.KOSPI, today, sectorInd, null, false);
            
            BenchmarkPrice market = mockBenchmark("0001", today, BigDecimal.valueOf(0.5));

            given(sectorInsightPort.findLatestBefore(eq("0007"), any(LocalDate.class))).willReturn(Optional.of(sector));
            given(benchmarkPricePort.findByTickerAndBaseDate("0001", today)).willReturn(Optional.of(market));

            SectorComparisonResult result = sectorInsightService.compareWithMarket("0007");

            assertThat(result.performanceStatus()).isEqualTo("OUTPERFORM");
        }

        @Test
        @DisplayName("RS가 0.4 이하(예: 0.3)이면 NEUTRAL을 반환한다")
        void returnsNeutral_whenRSBelowPointFour() {
            LocalDate today = LocalDate.now();
            SectorIndicators sectorInd = SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.0), 0L, 0L, 0, 0);
            SectorInsight sector = SectorInsight.of("전기전자", "0007", MarketType.KOSPI, today, sectorInd, null, false);
            
            BenchmarkPrice market = mockBenchmark("0001", today, BigDecimal.valueOf(0.7));

            given(sectorInsightPort.findLatestBefore(eq("0007"), any(LocalDate.class))).willReturn(Optional.of(sector));
            given(benchmarkPricePort.findByTickerAndBaseDate("0001", today)).willReturn(Optional.of(market));

            SectorComparisonResult result = sectorInsightService.compareWithMarket("0007");

            assertThat(result.performanceStatus()).isEqualTo("NEUTRAL");
        }

        @Test
        @DisplayName("최근 데이터가 없으면 예외를 던진다")
        void throwsException_whenNoDataExists() {
            given(sectorInsightPort.findLatestBefore(eq("0007"), any(LocalDate.class))).willReturn(Optional.empty());

            assertThatThrownBy(() -> sectorInsightService.compareWithMarket("0007"))
                    .isInstanceOf(SectorDomainException.class);
        }
    }

    @Nested
    @DisplayName("getTopSectorsByFluctuation")
    class GetTopSectorsByFluctuation {

        @Test
        @DisplayName("이름이 같은 섹터는 그룹화되어 평균 등락률로 반환된다")
        void groupsDuplicateNames_andCalculatesAverage() {
            // given
            LocalDate today = LocalDate.now();
            
            // "의료·정밀기기" (KOSPI)
            SectorIndicators ind1 = SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(3.92), 0L, 0L, 0, 0);
            SectorInsight s1 = SectorInsight.of("의료·정밀기기", "0014", MarketType.KOSPI, today, ind1, null, false);
            
            // "의료·정밀기기" (KOSDAQ) - 특수문자나 공백이 섞여도 그룹화되는지 확인
            SectorIndicators ind2 = SectorIndicators.of(BigDecimal.valueOf(2000), BigDecimal.valueOf(2.64), 0L, 0L, 0, 0);
            SectorInsight s2 = SectorInsight.of("의료 · 정밀 기기", "1029", MarketType.KOSDAQ, today, ind2, null, false);
            
            // 다른 섹터
            SectorIndicators ind3 = SectorIndicators.of(BigDecimal.valueOf(1500), BigDecimal.valueOf(3.41), 0L, 0L, 0, 0);
            SectorInsight s3 = SectorInsight.of("운송·창고", "1013", MarketType.KOSDAQ, today, ind3, null, false);

            given(sectorInsightPort.findAllByDate(today, null)).willReturn(List.of(s1, s2, s3));

            // when
            var results = sectorInsightService.getTopSectorsByFluctuation(today, null, 10);

            // then
            // 1. 결과는 2개여야 함 (의료·정밀기기 통합)
            assertThat(results).hasSize(2);
            
            // 2. "운송·창고"(3.41)가 1위, "의료·정밀기기"((3.92+2.64)/2 = 3.28)가 2위
            assertThat(results.get(0).sectorName()).isEqualTo("운송·창고");
            assertThat(results.get(0).fluctuationRate()).isEqualByComparingTo("3.41");
            
            assertThat(results.get(1).sectorName()).isEqualTo("의료·정밀기기");
            assertThat(results.get(1).fluctuationRate()).isEqualByComparingTo("3.2800"); // 4자리 반올림
        }
    }
}
