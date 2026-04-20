package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.insight.SectorIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class SectorInsightServiceTest {

    @InjectMocks
    private SectorInsightService sectorInsightService;

    @Mock
    private SectorInsightPort sectorInsightPort;

    @Test
    @DisplayName("섹터 이름이 Null인 데이터가 포함되어도 NPE가 발생하지 않아야 한다")
    void getTopSectorsByFluctuation_withNullName_shouldNotThrowNPE() {
        // given
        LocalDate today = LocalDate.now();
        SectorInsight nullNameInsight = SectorInsight.of(
                null, "CODE1", MarketType.KOSPI, today,
                SectorIndicators.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1.5), 0L, 0L, 0, 0),
                null, false
        );
        
        when(sectorInsightPort.findAllByDate(any(), any())).thenReturn(List.of(nullNameInsight));

        // when & then
        assertThatCode(() -> sectorInsightService.getTopSectorsByFluctuation(today, MarketType.KOSPI, 10))
                .doesNotThrowAnyException();
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
    @DisplayName("섹터 비교 요청 시 섹터 코드가 비어있는 경우 SectorDomainException이 발생해야 한다")
    void compareWithMarket_withEmptyCode_shouldThrowException() {
        assertThatThrownBy(() -> sectorInsightService.compareWithMarket(null))
                .isInstanceOf(SectorDomainException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }
}
