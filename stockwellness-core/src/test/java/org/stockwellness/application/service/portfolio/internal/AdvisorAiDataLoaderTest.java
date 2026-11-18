package org.stockwellness.application.service.portfolio.internal;

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
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private IndexDailyReadPort indexDailyReadPort;

    @Test
    @DisplayName("포트폴리오의 AI 분석 컨텍스트를 정상적으로 로드한다")
    void loadContext_success() {
        // given
        Long portfolioId = 1L;
        Portfolio portfolio = Portfolio.create(1L, "테스트", "");
        PortfolioItem item = PortfolioItem.createStock("005930", BigDecimal.TEN, new BigDecimal("150"), "KRW", new BigDecimal("100"), LocalDate.now());
        portfolio.updateItems(List.of(item));

        given(portfolioPort.findById(portfolioId)).willReturn(Optional.of(portfolio));
        
        AiAnalysisContext tech = mock(AiAnalysisContext.class);
        LocalDate tradeDate = LocalDate.now();
        given(tech.isinCode()).willReturn("005930");
        given(tech.baseDate()).willReturn(tradeDate);
        given(tech.priceInfo()).willReturn(new AiAnalysisContext.PriceSummary(new BigDecimal("150"), BigDecimal.ZERO, BigDecimal.ZERO));
        var input = new LoadTechnicalDataPort.CanonicalStockAnalysisInput(
                1L, "005930", "삼성전자", tradeDate, "KRX", 1, "{}", "test-v1", tech);
        given(loadTechnicalDataPort.loadTechnicalContexts(anyList())).willReturn(Map.of("005930", input));
        
        IndexPriceObservation benchmark = new IndexPriceObservation(1L, LocalDate.now(), null, null,
                new BigDecimal("2505"), null, null, null, null, null, 1, "FINAL");
        given(indexDailyReadPort.recentFinal("0001", LocalDate.now(), 2)).willReturn(List.of(benchmark));

        // when
        AdvisorAiContext context = dataLoader.loadContext(portfolioId);

        // then
        assertThat(context.portfolioName()).isEqualTo("테스트");
        assertThat(context.holdings()).hasSize(1);
        assertThat(context.holdings().get(0).ticker()).isEqualTo("005930");
        assertThat(context.holdings().get(0).name()).isEqualTo("삼성전자");
        assertThat(context.benchmarks()).hasSize(1);
        assertThat(context.benchmarks().get(0).name()).isEqualTo("KOSPI");
    }

    @Test
    @DisplayName("FINAL 종목 데이터가 없는 포트폴리오는 AI 입력으로 만들지 않는다")
    void loadContext_rejectsMissingCanonicalFinalPrice() {
        Portfolio portfolio = Portfolio.create(1L, "테스트", "");
        portfolio.updateItems(List.of(PortfolioItem.createStock("AAPL", BigDecimal.TEN,
                new BigDecimal("150"), "USD", new BigDecimal("100"), LocalDate.now())));
        given(portfolioPort.findById(1L)).willReturn(Optional.of(portfolio));
        given(loadTechnicalDataPort.loadTechnicalContexts(anyList())).willReturn(Map.of());

        assertThatThrownBy(() -> dataLoader.loadContext(1L)).isInstanceOf(StockDataNotFoundException.class);
    }
}
