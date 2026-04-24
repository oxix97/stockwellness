package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.command.StockAnalysisCommand;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockAnalysisService 단위 테스트")
class StockAnalysisServiceTest {

    @InjectMocks
    private StockAnalysisService stockAnalysisService;

    @Mock
    private LoadTechnicalDataPort technicalDataPort;

    @Mock
    private LlmClientPort llmClientPort;

    @Test
    @DisplayName("종목 분석 요청 시 기술 컨텍스트를 로드하고 LLM 분석 결과를 반환한다")
    void analyze_Success() {
        StockAnalysisCommand command = new StockAnalysisCommand("005930");
        AiAnalysisContext context = createContext("005930", TrendStatus.REGULAR);
        AiReport report = new AiReport(
                InvestmentDecision.BUY,
                85,
                "정배열 상승",
                List.of("정배열", "RSI 안정", "거래량 증가"),
                "상승 추세가 확인됩니다."
        );
        given(technicalDataPort.loadTechnicalContext("005930")).willReturn(context);
        given(llmClientPort.generateInsight(anyString(), org.mockito.ArgumentMatchers.eq(context))).willReturn(report);

        StockAnalysisResult result = stockAnalysisService.analyze(command);

        assertThat(result.isinCode()).isEqualTo("005930");
        assertThat(result.trendStatus()).isEqualTo(TrendStatus.REGULAR);
        assertThat(result.report()).isSameAs(report);
        assertThat(result.analyzedAt()).isNotNull();
        verify(technicalDataPort).loadTechnicalContext("005930");
        verify(llmClientPort).generateInsight(anyString(), org.mockito.ArgumentMatchers.eq(context));
    }

    @Test
    @DisplayName("LLM 호출 시 서비스의 시스템 지시문과 기술 컨텍스트를 함께 전달한다")
    void analyze_PassesSystemInstructionAndContext() {
        StockAnalysisCommand command = new StockAnalysisCommand("000660");
        AiAnalysisContext context = createContext("000660", TrendStatus.NEUTRAL);
        AiReport report = AiReport.fallback();
        given(technicalDataPort.loadTechnicalContext("000660")).willReturn(context);
        given(llmClientPort.generateInsight(anyString(), org.mockito.ArgumentMatchers.eq(context))).willReturn(report);

        stockAnalysisService.analyze(command);

        ArgumentCaptor<String> instructionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClientPort).generateInsight(instructionCaptor.capture(), org.mockito.ArgumentMatchers.eq(context));
        assertThat(instructionCaptor.getValue())
                .contains("퀀트 트레이더")
                .contains("JSON 포맷")
                .contains("decision")
                .contains("confidenceScore");
    }

    @Test
    @DisplayName("기술 데이터 로드 실패 시 예외를 전파하고 LLM을 호출하지 않는다")
    void analyze_PropagatesTechnicalDataFailure() {
        StockAnalysisCommand command = new StockAnalysisCommand("005930");
        RuntimeException exception = new IllegalStateException("기술 데이터 없음");
        given(technicalDataPort.loadTechnicalContext("005930")).willThrow(exception);

        assertThatThrownBy(() -> stockAnalysisService.analyze(command))
                .isSameAs(exception);
        verify(llmClientPort, never()).generateInsight(anyString(), any());
    }

    @Test
    @DisplayName("LLM 분석 실패 시 예외를 전파한다")
    void analyze_PropagatesLlmFailure() {
        StockAnalysisCommand command = new StockAnalysisCommand("005930");
        AiAnalysisContext context = createContext("005930", TrendStatus.INVERSE);
        RuntimeException exception = new IllegalStateException("LLM 장애");
        given(technicalDataPort.loadTechnicalContext("005930")).willReturn(context);
        given(llmClientPort.generateInsight(anyString(), org.mockito.ArgumentMatchers.eq(context))).willThrow(exception);

        assertThatThrownBy(() -> stockAnalysisService.analyze(command))
                .isSameAs(exception);
    }

    private AiAnalysisContext createContext(String isinCode, TrendStatus trendStatus) {
        return new AiAnalysisContext(
                isinCode,
                LocalDate.of(2026, 4, 24),
                new AiAnalysisContext.PriceSummary(
                        BigDecimal.valueOf(80000),
                        BigDecimal.valueOf(2.5),
                        BigDecimal.valueOf(1_000_000)
                ),
                new AiAnalysisContext.TechnicalSignal(
                        trendStatus,
                        BigDecimal.valueOf(55),
                        "중립",
                        BigDecimal.valueOf(1.2),
                        CrossoverSignal.NONE,
                        BigDecimal.valueOf(79000),
                        BigDecimal.valueOf(78000),
                        BigDecimal.valueOf(76000),
                        BigDecimal.valueOf(74000)
                ),
                new AiAnalysisContext.PortfolioRisk(false, 10.0)
        );
    }
}
