package org.stockwellness.batch.job.sector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorEodBatchServiceTest {

    @InjectMocks
    private SectorEodBatchService sectorEodBatchService;

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
    @DisplayName("prepareSync 이후 여러 syncSector 호출은 선로딩된 캐시만 사용한다")
    void syncSector_usesPreparedCaches() {
        LocalDate targetDate = LocalDate.of(2026, 4, 9);
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.of("0001", "0029", null, "전기전자"), StockStatus.ACTIVE);
        StockPrice price = StockPrice.of(stock, targetDate, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 100L, BigDecimal.TEN, null);
        MarketIndex marketIndex = MarketIndex.of("0029", "전기전자");
        SectorInsight previousInsight = SectorInsight.of("전기전자", "0029", MarketType.KOSPI, targetDate.minusDays(1),
                SectorIndicators.of(BigDecimal.valueOf(900), BigDecimal.ZERO, 1L, 1L, 1, 1), null, false);
        SectorInsight analyzed = SectorInsight.of("전기전자", "0029", MarketType.KOSPI, targetDate,
                SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.ZERO, 2L, 2L, 2, 2), null, false);

        given(stockPricePort.findAllByDate(targetDate)).willReturn(List.of(price));
        given(stockPort.findBySectorMediumCode(null)).willReturn(List.of(stock));
        given(marketIndexPort.findAll()).willReturn(List.of(marketIndex));
        given(sectorInsightPort.findLatestBeforeByCodes(List.of("0029"), targetDate)).willReturn(java.util.Map.of("0029", previousInsight));
        given(sectorInsightPort.findPastPricesByCodes(List.of("0029"), targetDate, 119))
                .willReturn(java.util.Map.of("0029", List.of(BigDecimal.valueOf(900), BigDecimal.valueOf(800))));
        given(sectorAnalysisService.analyze(eq(marketIndex), any(), eq(previousInsight), eq(List.of(BigDecimal.valueOf(900), BigDecimal.valueOf(800))), eq(List.of(price))))
                .willReturn(analyzed);

        sectorEodBatchService.prepareSync(targetDate, List.of("0029"));

        SectorApiDto apiDto = new SectorApiDto("0029", "전기전자", targetDate, BigDecimal.valueOf(1000), BigDecimal.ONE, 10L, 20L);
        SectorEodSyncUseCase.SectorEodResult first = sectorEodBatchService.syncSector(new SectorEodSyncUseCase.SectorSyncCommand(apiDto));
        SectorEodSyncUseCase.SectorEodResult second = sectorEodBatchService.syncSector(new SectorEodSyncUseCase.SectorSyncCommand(apiDto));

        assertThat(first.sectorInsight()).isSameAs(analyzed);
        assertThat(second.sectorInsight()).isSameAs(analyzed);
        verify(sectorInsightPort, times(1)).findLatestBeforeByCodes(List.of("0029"), targetDate);
        verify(sectorInsightPort, times(1)).findPastPricesByCodes(List.of("0029"), targetDate, 119);
        verify(sectorInsightPort, never()).findBySectorCodeAndDate(any(), any());
    }

    @Test
    @DisplayName("매핑된 지수가 없으면 null 결과를 반환한다")
    void syncSector_returnsNullWhenIndexMissing() {
        LocalDate targetDate = LocalDate.of(2026, 4, 9);
        given(stockPricePort.findAllByDate(targetDate)).willReturn(List.of());
        given(stockPort.findBySectorMediumCode(null)).willReturn(List.of());
        given(marketIndexPort.findAll()).willReturn(List.of());
        given(sectorInsightPort.findLatestBeforeByCodes(List.of("9999"), targetDate)).willReturn(java.util.Map.of());
        given(sectorInsightPort.findPastPricesByCodes(List.of("9999"), targetDate, 119)).willReturn(java.util.Map.of());

        SectorEodSyncUseCase.SectorEodResult result = sectorEodBatchService.syncSector(
                new SectorEodSyncUseCase.SectorSyncCommand(
                        new SectorApiDto("9999", "미정", targetDate, BigDecimal.ONE, BigDecimal.ZERO, 0L, 0L)
                )
        );

        assertThat(result.sectorInsight()).isNull();
    }
}
