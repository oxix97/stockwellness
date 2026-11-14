package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.SectorMarketObservation;
import org.stockwellness.application.port.out.stock.SectorMarketReadPort;
import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.domain.stock.analysis.DiagnosisStatus;
import org.stockwellness.global.error.ErrorCode;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorInsightServiceTest {

    @InjectMocks
    private SectorInsightService sectorInsightService;

    @Mock
    private SectorMarketReadPort sectorMarketReadPort;

    @Mock
    private LoadBenchmarkPort loadBenchmarkPort;

    @Test
    @DisplayName("섹터 이름이 Null인 데이터가 포함되어도 NPE가 발생하지 않아야 한다")
    void getTopSectorsByFluctuation_withNullName_shouldNotThrowNPE() {
        // given
        LocalDate today = LocalDate.now();
        SectorMarketObservation nullNameInsight = new SectorMarketObservation(1L, "CODE1", null, "KOSPI", today,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1.5), null, null, null);
        when(sectorMarketReadPort.finalOnDate(today, MarketType.KOSPI.name())).thenReturn(List.of(nullNameInsight));

        // when & then
        assertThatCode(() -> sectorInsightService.getTopSectorsByFluctuation(today, MarketType.KOSPI, 10))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("명시 날짜 데이터가 없으면 최근 FINAL 날짜에서 표기를 정규화해 섹터를 묶는다")
    void getTopSectorsByFluctuation_fallsBackAndGroupsCanonicalRows() {
        LocalDate latestDate = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinalDate("KOSPI")).thenReturn(Optional.of(latestDate));
        when(sectorMarketReadPort.finalOnDate(latestDate, "KOSPI")).thenReturn(List.of(
                sector(1, "SEMI", "반도체·IT", "KOSPI", latestDate, "100", "2", "90", "55"),
                sector(2, "SEMI2", "반도체 IT", "KOSPI", latestDate, "200", "4", "180", "55"),
                sector(3, "ENERGY", "2차전지", "KOSPI", latestDate, "80", "-1", "80", "50"),
                new SectorMarketObservation(4, "EMPTY", null, "KOSPI", latestDate,
                        BigDecimal.TEN, BigDecimal.ONE, null, null, null),
                new SectorMarketObservation(5, "EMPTY_PRICE", "가격누락", "KOSPI", latestDate,
                        null, BigDecimal.ONE, null, null, null),
                new SectorMarketObservation(6, "EMPTY_RATE", "등락누락", "KOSPI", latestDate,
                        BigDecimal.TEN, null, null, null, null)));

        var result = sectorInsightService.getTopSectorsByFluctuation(null, MarketType.KOSPI, 10);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().sectorName()).isEqualTo("반도체·IT");
        assertThat(result.getFirst().currentPrice()).isEqualByComparingTo("150");
        assertThat(result.getFirst().fluctuationRate()).isEqualByComparingTo("3.0000");
        assertThat(result.getFirst().isOverheated()).isTrue();
        assertThat(result.getLast().fluctuationRate()).isEqualByComparingTo("-1");
    }

    @Test
    @DisplayName("섹터 FINAL 날짜를 찾지 못하면 빈 랭킹을 반환한다")
    void getTopSectorsByFluctuation_returnsEmptyWhenNoFinalDateExists() {
        when(sectorMarketReadPort.latestFinalDate(null)).thenReturn(Optional.empty());

        assertThat(sectorInsightService.getTopSectorsByFluctuation(null, null, 10)).isEmpty();
    }

    @Test
    @DisplayName("섹터 코드가 비어있는 경우 SectorDomainException이 발생해야 한다")
    void getSectorDetail_withEmptyCode_shouldThrowException() {
        LocalDate today = LocalDate.now();
        assertThatThrownBy(() -> sectorInsightService.getSectorDetail("", today))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("섹터 상세는 새 구조의 FINAL 지수와 주도주만 반환한다")
    void getSectorDetail_usesCanonicalFinalSectorData() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        SectorMarketObservation observation = new SectorMarketObservation(3L, "SEMI", "반도체", "KOSPI", date,
                BigDecimal.valueOf(110), BigDecimal.TEN, BigDecimal.valueOf(108), BigDecimal.valueOf(100),
                BigDecimal.valueOf(55));
        LeadingStock leader = new LeadingStock("삼성전자", "005930", BigDecimal.valueOf(5), 100L, BigDecimal.TEN);
        when(sectorMarketReadPort.latestFinal("SEMI", date)).thenReturn(java.util.Optional.of(observation));
        when(sectorMarketReadPort.leadingStocks(3L, date)).thenReturn(List.of(leader));

        var result = sectorInsightService.getSectorDetail("SEMI", date);

        assertThat(result.baseDate()).isEqualTo(date);
        assertThat(result.fluctuationRate()).isEqualByComparingTo("10");
        assertThat(result.leadingStocks()).containsExactly(leader);
        assertThat(result.aiOpinion()).isNull();
    }

    @Test
    @DisplayName("섹터 지표가 부족하면 상세 진단을 데이터 부족으로 표시한다")
    void getSectorDetail_reportsInsufficientDataWhenIndicatorsAreIncomplete() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        SectorMarketObservation observation = new SectorMarketObservation(3L, "SEMI", "반도체", "KOSPI", date,
                BigDecimal.valueOf(110), BigDecimal.TEN, null, null, BigDecimal.valueOf(55));
        when(sectorMarketReadPort.latestFinal("SEMI", date)).thenReturn(java.util.Optional.of(observation));

        var result = sectorInsightService.getSectorDetail("SEMI", date);

        assertThat(result.diagnosisMessage()).isEqualTo(
                org.stockwellness.domain.stock.analysis.DiagnosisStatus.DATA_INSUFFICIENT.getMessage());
    }

    @Test
    @DisplayName("섹터 상세 진단은 RSI와 20일선 조건별 과열·침체 상태를 구분한다")
    void getSectorDetail_classifiesTechnicalDiagnosis() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        Map<String, SectorMarketObservation> rows = Map.of(
                "BOTH", sector(1, "BOTH", "복합 과열", "KOSPI", date, "120", "1", "100", "75"),
                "RSI", sector(2, "RSI", "RSI 과열", "KOSPI", date, "100", "1", "100", "75"),
                "DISPARITY", sector(3, "DISPARITY", "이격 과열", "KOSPI", date, "115", "1", "100", "50"),
                "STAGNANT", sector(4, "STAGNANT", "침체", "KOSPI", date, "100", "1", "100", "20"),
                "NORMAL", sector(5, "NORMAL", "정상", "KOSPI", date, "100", "1", "100", "50"),
                "ZERO_MA", sector(6, "ZERO_MA", "0 이동평균", "KOSPI", date, "100", "1", "0", "50"));
        when(sectorMarketReadPort.latestFinal(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(date)))
                .thenAnswer(call -> Optional.ofNullable(rows.get(call.getArgument(0))));

        assertThat(sectorInsightService.getSectorDetail("BOTH", date).diagnosisMessage())
                .isEqualTo(DiagnosisStatus.OVERHEATED_BOTH.getMessage());
        assertThat(sectorInsightService.getSectorDetail("RSI", date).diagnosisMessage())
                .isEqualTo(DiagnosisStatus.OVERHEATED_RSI.getMessage());
        assertThat(sectorInsightService.getSectorDetail("DISPARITY", date).diagnosisMessage())
                .isEqualTo(DiagnosisStatus.OVERHEATED_DISPARITY.getMessage());
        assertThat(sectorInsightService.getSectorDetail("STAGNANT", date).diagnosisMessage())
                .isEqualTo(DiagnosisStatus.STAGNANT.getMessage());
        assertThat(sectorInsightService.getSectorDetail("NORMAL", date).diagnosisMessage())
                .isEqualTo(DiagnosisStatus.NORMAL.getMessage());
        var zeroMa = sectorInsightService.getSectorDetail("ZERO_MA", date);
        assertThat(zeroMa.diagnosisMessage()).isEqualTo(DiagnosisStatus.NORMAL.getMessage());
        assertThat(zeroMa.isOverheated()).isFalse();
    }

    @Test
    @DisplayName("섹터 상세 데이터가 없으면 도메인 예외를 반환한다")
    void getSectorDetail_throwsWhenNoFinalObservationExists() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinal("MISSING", date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorInsightService.getSectorDetail("MISSING", date))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.SECTOR_DATA_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("섹터 비교는 새 FINAL 지수와 같은 거래일의 시장 등락률로 상대강도를 계산한다")
    void compareWithMarket_usesCanonicalFinalMarketHistory() {
        LocalDate firstDate = LocalDate.of(2026, 11, 10);
        LocalDate lastDate = LocalDate.of(2026, 11, 11);
        SectorMarketObservation latest = new SectorMarketObservation(3L, "SEMI", "반도체", "KOSPI", lastDate,
                BigDecimal.valueOf(110), BigDecimal.valueOf(10), null, null, null);
        when(sectorMarketReadPort.latestFinal(eq("SEMI"), any())).thenReturn(java.util.Optional.of(latest));
        when(loadBenchmarkPort.loadBenchmarkPriceAtOrBefore("0001", lastDate))
                .thenReturn(java.util.Optional.of(price(lastDate, "105", "5")));
        when(sectorMarketReadPort.historyFinal(3L, lastDate, 30)).thenReturn(List.of(
                new SectorMarketObservation(3L, "SEMI", "반도체", "KOSPI", lastDate,
                        BigDecimal.valueOf(110), BigDecimal.valueOf(10), null, null, null),
                new SectorMarketObservation(3L, "SEMI", "반도체", "KOSPI", firstDate,
                        BigDecimal.valueOf(100), BigDecimal.valueOf(2), null, null, null)));
        when(loadBenchmarkPort.loadBenchmarkPrices("0001", firstDate, lastDate)).thenReturn(List.of(
                price(firstDate, "100", "1"), price(lastDate, "105", "5")));

        var result = sectorInsightService.compareWithMarket("SEMI");

        assertThat(result.relativeStrength()).isEqualByComparingTo("5");
        assertThat(result.historicalComparison()).hasSize(2);
        assertThat(result.historicalComparison().getLast().relativeStrength()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("지원하지 않는 섹터 거래소는 KOSPI 기준으로 잘못 비교하지 않는다")
    void compareWithMarket_rejectsUnsupportedMarket() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinal(eq("SEMI"), any())).thenReturn(java.util.Optional.of(
                new SectorMarketObservation(3L, "SEMI", "반도체", "NASDAQ", date,
                        BigDecimal.valueOf(110), BigDecimal.TEN, null, null, null)));

        assertThatThrownBy(() -> sectorInsightService.compareWithMarket("SEMI"))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.SECTOR_DATA_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("섹터 시장 비교는 벤치마크 데이터가 없으면 도메인 예외를 반환한다")
    void compareWithMarket_throwsWhenBenchmarkIsMissing() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinal(eq("SEMI"), any())).thenReturn(Optional.of(
                sector(3, "SEMI", "반도체", "KOSPI", date, "110", "1", null, null)));
        when(loadBenchmarkPort.loadBenchmarkPriceAtOrBefore("0001", date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorInsightService.compareWithMarket("SEMI"))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.SECTOR_DATA_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("섹터나 벤치마크 등락률이 없으면 상대강도를 계산하지 않는다")
    void compareWithMarket_throwsWhenChangeRateIsMissing() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinal(eq("SEMI"), any())).thenReturn(Optional.of(
                sector(3, "SEMI", "반도체", "KOSPI", date, "110", null, null, null)));
        when(loadBenchmarkPort.loadBenchmarkPriceAtOrBefore("0001", date)).thenReturn(Optional.of(
                price(date, "105", "5")));

        assertThatThrownBy(() -> sectorInsightService.compareWithMarket("SEMI"))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.SECTOR_DATA_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비교 이력이 비어 있어도 최신 상대강도는 계산하고 중립 상태를 반환한다")
    void compareWithMarket_returnsCurrentComparisonWhenHistoryIsEmpty() {
        LocalDate date = LocalDate.of(2026, 11, 11);
        when(sectorMarketReadPort.latestFinal(eq("SEMI"), any())).thenReturn(Optional.of(
                sector(3, "SEMI", "반도체", "KOSDAQ", date, "110", "1", null, null)));
        when(loadBenchmarkPort.loadBenchmarkPriceAtOrBefore("1001", date))
                .thenReturn(Optional.of(price(date, "105", "0.8")));
        when(sectorMarketReadPort.historyFinal(3L, date, 30)).thenReturn(List.of());
        when(loadBenchmarkPort.loadBenchmarkPrices("1001", date, date)).thenReturn(List.of());

        var result = sectorInsightService.compareWithMarket("SEMI");

        assertThat(result.relativeStrength()).isEqualByComparingTo("0.2");
        assertThat(result.performanceStatus()).isEqualTo("NEUTRAL");
        assertThat(result.historicalComparison()).isEmpty();
    }

    @Test
    @DisplayName("섹터 비교 요청 시 섹터 코드가 비어있는 경우 SectorDomainException이 발생해야 한다")
    void compareWithMarket_withEmptyCode_shouldThrowException() {
        assertThatThrownBy(() -> sectorInsightService.compareWithMarket(null))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("섹터 원천 배치는 상위 orchestration으로 위임한다")
    void syncAllSectorInsights_doesNotWriteLegacyData() {
        sectorInsightService.syncAllSectorInsights();
    }

    private static SectorMarketObservation sector(
            long seriesId, String code, String name, String market, LocalDate date,
            String close, String change, String ma20, String rsi) {
        return new SectorMarketObservation(seriesId, code, name, market, date,
                decimal(close), decimal(change), null, decimal(ma20), decimal(rsi));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static StockPriceResult price(LocalDate date, String close, String changeRate) {
        BigDecimal value = new BigDecimal(close);
        return new StockPriceResult(date, value, value, value, value, value, 100L, BigDecimal.TEN,
                null, null, null, null, new BigDecimal(changeRate), "0001");
    }
}
