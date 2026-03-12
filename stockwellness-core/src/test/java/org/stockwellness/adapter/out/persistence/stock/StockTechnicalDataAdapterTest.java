package org.stockwellness.adapter.out.persistence.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockPriceId;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockTechnicalDataAdapter 단위 테스트")
class StockTechnicalDataAdapterTest {

    @InjectMocks
    private StockTechnicalDataAdapter adapter;

    @Mock
    private StockPriceRepository stockPriceRepository;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = mock(Stock.class);
        given(stock.getTicker()).willReturn("AAPL");
    }

    @Test
    @DisplayName("종목의 기술적 분석 컨텍스트를 로드한다")
    void loadTechnicalContext_success() {
        // given
        LocalDate today = LocalDate.now();
        StockPrice price = mock(StockPrice.class);
        StockPriceId id = new StockPriceId(today, 1L);
        
        given(price.getStock()).willReturn(stock);
        given(price.getId()).willReturn(id);
        given(price.getClosePrice()).willReturn(new BigDecimal("150.00"));
        
        TechnicalIndicators indicators = new TechnicalIndicators(
                new BigDecimal("145.00"), new BigDecimal("140.00"), new BigDecimal("130.00"), new BigDecimal("120.00"),
                new BigDecimal("65.0"), new BigDecimal("2.5"), new BigDecimal("2.0"),
                null, null, null, null, null, null, null, null, null, null
        );
        given(price.getIndicators()).willReturn(indicators);
        
        given(stockPriceRepository.findRecentPrices(eq("AAPL"), any(), any(Pageable.class)))
                .willReturn(List.of(price));

        // when
        AiAnalysisContext context = adapter.loadTechnicalContext("AAPL");

        // then
        assertThat(context.isinCode()).isEqualTo("AAPL");
        assertThat(context.technicalSignal().rsi()).isEqualTo(new BigDecimal("65.0"));
        assertThat(context.technicalSignal().trendStatus().name()).isEqualTo("REGULAR"); // 145 > 140 > 130 > 120
    }

    @Test
    @DisplayName("여러 종목의 기술적 분석 컨텍스트를 배치로 로드한다")
    void loadTechnicalContexts_success() {
        // given
        StockPrice price = mock(StockPrice.class);
        given(price.getStock()).willReturn(stock);
        given(price.getId()).willReturn(new StockPriceId(LocalDate.now(), 1L));
        given(price.getIndicators()).willReturn(TechnicalIndicators.empty());
        given(price.getClosePrice()).willReturn(BigDecimal.ZERO);

        given(stockPriceRepository.findRecentPricesByTickers(eq(List.of("AAPL")), any()))
                .willReturn(List.of(price));

        // when
        Map<String, AiAnalysisContext> result = adapter.loadTechnicalContexts(List.of("AAPL"));

        // then
        assertThat(result).containsKey("AAPL");
    }
}
