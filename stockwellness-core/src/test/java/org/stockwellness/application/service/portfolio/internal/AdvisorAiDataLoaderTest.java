package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvisorAiDataLoader 단위 테스트")
class AdvisorAiDataLoaderTest {

    @InjectMocks
    private AdvisorAiDataLoader dataLoader;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private LoadTechnicalDataPort loadTechnicalDataPort;

    @Mock
    private LoadBenchmarkPort loadBenchmarkPort;

    @Test
    @DisplayName("포트폴리오의 AI 분석 컨텍스트를 정상적으로 로드한다")
    void loadContext_success() {
        // given
        Long portfolioId = 1L;
        Portfolio portfolio = Portfolio.create(1L, "테스트", "");
        PortfolioItem item = PortfolioItem.createStock("AAPL", BigDecimal.TEN, new BigDecimal("150"), "USD", new BigDecimal("100"));
        portfolio.updateItems(List.of(item));

        given(portfolioPort.findById(portfolioId)).willReturn(Optional.of(portfolio));
        
        AiAnalysisContext tech = mock(AiAnalysisContext.class);
        given(tech.priceInfo()).willReturn(new AiAnalysisContext.PriceSummary(new BigDecimal("150"), BigDecimal.ZERO, BigDecimal.ZERO));
        given(loadTechnicalDataPort.loadTechnicalContexts(anyList())).willReturn(Map.of("AAPL", tech));
        
        StockPriceResult benchmark = new StockPriceResult(LocalDate.now(), new BigDecimal("2500"), new BigDecimal("2510"), new BigDecimal("2490"), new BigDecimal("2505"), new BigDecimal("2505"), 1000000L);
        given(loadBenchmarkPort.loadBenchmarkPrices(eq("KOSPI"), any(), any())).willReturn(List.of(benchmark));

        // when
        AdvisorAiContext context = dataLoader.loadContext(portfolioId);

        // then
        assertThat(context.portfolioName()).isEqualTo("테스트");
        assertThat(context.holdings()).hasSize(1);
        assertThat(context.holdings().get(0).ticker()).isEqualTo("AAPL");
        assertThat(context.benchmarks()).hasSize(1);
        assertThat(context.benchmarks().get(0).name()).isEqualTo("KOSPI");
    }
}
