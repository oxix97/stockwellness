package org.stockwellness.batch.job.stock.sector.job.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SectorInsightItemProcessorTest {

    private final MarketIndexPort marketIndexPort = mock(MarketIndexPort.class);
    private final SectorInsightPort sectorInsightPort = mock(SectorInsightPort.class);
    private final StockPort stockPort = mock(StockPort.class);
    private final StockPricePort stockPricePort = mock(StockPricePort.class);
    private final SectorAnalysisService sectorAnalysisService = mock(SectorAnalysisService.class);

    private final SectorInsightItemProcessor processor = new SectorInsightItemProcessor(
            marketIndexPort, sectorInsightPort, stockPort, stockPricePort, sectorAnalysisService
    );

    @Test
    @DisplayName("SectorApiDto 처리 시 캐시 초기화 및 서비스 호출 검증")
    void processDtoAndVerifyCaching() {
        // given
        LocalDate today = LocalDate.now();
        String sectorCode = "001";
        SectorApiDto apiDto = new SectorApiDto(sectorCode, "전기전자", today, BigDecimal.valueOf(2500), BigDecimal.valueOf(1.0), 1000L, 500L);

        MarketIndex index = new MarketIndex(sectorCode, "전기전자");
        when(marketIndexPort.findAll()).thenReturn(List.of(index));

        // mediumCode를 sectorCode("001")와 일치시켜 매핑 로직이 정상 작동하게 함
        Stock stock = Stock.of("T001", "KR001", "테스트주", MarketType.KOSPI, Currency.KRW, StockSector.of("IT", sectorCode, "001", "IT"), StockStatus.ACTIVE);
        when(stockPort.findBySectorMediumCode(null)).thenReturn(List.of(stock));

        StockPrice price = mock(StockPrice.class);
        when(price.getStock()).thenReturn(stock);
        when(stockPricePort.findAllByDate(any(LocalDate.class))).thenReturn(List.of(price));

        when(sectorInsightPort.findBySectorCodeAndDate(anyString(), any(LocalDate.class))).thenReturn(Optional.empty());
        
        when(sectorInsightPort.findPastPricesByCodes(anyList(), any(LocalDate.class), anyInt()))
                .thenReturn(Map.of(sectorCode, List.of(BigDecimal.valueOf(2400))));
        when(sectorInsightPort.findLatestBeforeByCodes(anyList(), any(LocalDate.class)))
                .thenReturn(Map.of(sectorCode, mock(SectorInsight.class)));
        
        SectorInsight expectedResult = mock(SectorInsight.class);
        when(sectorAnalysisService.analyze(any(), any(), any(), any(), any())).thenReturn(expectedResult);

        // when
        SectorInsight result1 = processor.process(apiDto);
        SectorInsight result2 = processor.process(apiDto);

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1).isEqualTo(expectedResult);
        assertThat(result2).isEqualTo(expectedResult);

        verify(marketIndexPort, times(1)).findAll();
        verify(stockPort, times(1)).findBySectorMediumCode(null);
        verify(stockPricePort, times(1)).findAllByDate(any(LocalDate.class));
    }
}
