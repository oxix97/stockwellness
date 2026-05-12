package org.stockwellness.application.service.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectorEodBatchService 단위 테스트")
class SectorEodBatchServiceTest {

    @Mock
    private MarketIndexPort marketIndexPort;

    @Mock
    private SectorInsightPort sectorInsightPort;

    @Mock
    private StockPort stockPort;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private SectorAnalysisService sectorAnalysisService;

    @Mock
    private LoadSectorAiPort loadSectorAiPort;

    @Test
    @DisplayName("섹터 코드는 trim과 중복 제거 후 선로딩한다")
    void prepareSync_NormalizesSectorCodes() {
        SectorEodBatchService service = service();
        LocalDate targetDate = LocalDate.of(2026, 4, 24);
        given(stockPricePort.findAllByDate(targetDate)).willReturn(List.of());
        given(stockPort.findBySectorCode(null)).willReturn(List.of());
        given(marketIndexPort.findAll()).willReturn(List.of());
        given(sectorInsightPort.findLatestBeforeByCodes(anyList(), eq(targetDate))).willReturn(Map.of());
        given(sectorInsightPort.findPastPricesByCodes(anyList(), eq(targetDate), eq(119))).willReturn(Map.of());

        service.prepareSync(targetDate, Arrays.asList(" 0029 ", "0029", "", "  ", "0027", null));

        ArgumentCaptor<List<String>> codeCaptor = ArgumentCaptor.forClass(List.class);
        verify(sectorInsightPort).findLatestBeforeByCodes(codeCaptor.capture(), eq(targetDate));
        assertThat(codeCaptor.getValue()).containsExactlyInAnyOrder("0029", "0027");
        verify(sectorInsightPort).findPastPricesByCodes(codeCaptor.capture(), eq(targetDate), eq(119));
        assertThat(codeCaptor.getValue()).containsExactlyInAnyOrder("0029", "0027");
    }

    @Test
    @DisplayName("syncSector는 캐시 데이터로 분석 서비스를 호출한다")
    void syncSector_DelegatesWithCachedSectorData() {
        SectorEodBatchService service = service();
        LocalDate targetDate = LocalDate.of(2026, 4, 24);
        MarketIndex marketIndex = MarketIndex.of("0029", "전기전자");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.of("0001", "0029", null, "전기전자"), StockStatus.ACTIVE);
        StockPrice price = StockPrice.of(stock, targetDate, BigDecimal.valueOf(100), BigDecimal.valueOf(110),
                BigDecimal.valueOf(90), BigDecimal.valueOf(105), BigDecimal.valueOf(101), BigDecimal.valueOf(2),
                1_000L, BigDecimal.valueOf(2), null);
        SectorInsight previousInsight = sectorInsight(targetDate.minusDays(1));
        SectorInsight analyzed = sectorInsight(targetDate);
        List<BigDecimal> pastPrices = List.of(BigDecimal.valueOf(900), BigDecimal.valueOf(920));

        given(stockPricePort.findAllByDate(targetDate)).willReturn(List.of(price));
        given(stockPort.findBySectorCode(null)).willReturn(List.of(stock));
        given(marketIndexPort.findAll()).willReturn(List.of(marketIndex));
        given(sectorInsightPort.findLatestBeforeByCodes(List.of("0029"), targetDate)).willReturn(Map.of("0029", previousInsight));
        given(sectorInsightPort.findPastPricesByCodes(List.of("0029"), targetDate, 119)).willReturn(Map.of("0029", pastPrices));
        given(sectorAnalysisService.analyze(eq(marketIndex), any(), eq(previousInsight), eq(pastPrices), eq(List.of(price))))
                .willReturn(analyzed);

        SectorEodSyncUseCase.SectorEodResult result = service.syncSector(
                new SectorEodSyncUseCase.SectorSyncCommand(sectorApiDto(targetDate))
        );

        assertThat(result.sectorInsight()).isSameAs(analyzed);
        verify(sectorAnalysisService).analyze(eq(marketIndex), any(SectorApiDto.class), eq(previousInsight), eq(pastPrices), eq(List.of(price)));
    }

    @Test
    @DisplayName("AI 비활성화 상태에서는 기존 섹터 인사이트를 그대로 반환한다")
    void enrichAiOpinion_ReturnsInsightWithoutAiWhenDisabled() {
        SectorEodBatchService service = service();
        ReflectionTestUtils.setField(service, "aiEnabled", false);
        SectorInsight insight = sectorInsight(LocalDate.of(2026, 4, 24));

        SectorEodSyncUseCase.SectorEodResult result = service.enrichAiOpinion(
                new SectorEodSyncUseCase.SectorAiAnalysisCommand(insight)
        );

        assertThat(result.sectorInsight()).isSameAs(insight);
        verify(loadSectorAiPort, never()).generateSectorOpinion(any());
    }

    @Test
    @DisplayName("섹터 인사이트가 없으면 AI 보강도 null 결과를 반환한다")
    void enrichAiOpinion_ReturnsNullWhenInsightMissing() {
        SectorEodBatchService service = service();

        SectorEodSyncUseCase.SectorEodResult result = service.enrichAiOpinion(
                new SectorEodSyncUseCase.SectorAiAnalysisCommand(null)
        );

        assertThat(result.sectorInsight()).isNull();
        verify(loadSectorAiPort, never()).generateSectorOpinion(any());
    }

    private SectorEodBatchService service() {
        return new SectorEodBatchService(
                marketIndexPort,
                sectorInsightPort,
                stockPort,
                stockPricePort,
                sectorAnalysisService,
                loadSectorAiPort
        );
    }

    private SectorApiDto sectorApiDto(LocalDate targetDate) {
        return new SectorApiDto(
                "0029",
                "전기전자",
                targetDate,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1.2),
                10L,
                20L
        );
    }

    private SectorInsight sectorInsight(LocalDate baseDate) {
        return SectorInsight.of(
                "전기전자",
                "0029",
                MarketType.KOSPI,
                baseDate,
                SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.2), 10L, 20L, 2, 1),
                null,
                false
        );
    }
}
