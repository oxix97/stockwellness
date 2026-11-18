package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.command.StockAnalysisCommand;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort.CanonicalStockAnalysisInput;
import org.stockwellness.application.port.out.stock.StockAiAnalysisPort;
import org.stockwellness.application.port.out.stock.StockAiAnalysisPort.StockAiAnalysisRecord;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.analysis.TrendStatus;
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

    @Mock
    private StockAiAnalysisPort stockAiAnalysisPort;

    @Test
    @DisplayName("종목 분석 요청 시 기술 컨텍스트를 로드하고 LLM 분석 결과를 반환한다")
    void analyze_Success() {
        StockAnalysisCommand command = new StockAnalysisCommand("005930");
        CanonicalStockAnalysisInput input = createInput("005930", TrendStatus.REGULAR);
        AiAnalysisContext context = input.context();
        AiReport report = new AiReport(
                InvestmentDecision.BUY,
                85,
                "정배열 상승",
                List.of("정배열", "RSI 안정", "거래량 증가"),
                "상승 추세가 확인됩니다."
        );
        given(technicalDataPort.loadTechnicalContext("005930")).willReturn(input);
        given(llmClientPort.generateInsight(anyString(), ArgumentMatchers.eq(context)))
                .willReturn(new LlmClientPort.AiGenerationResult(report, "gpt-4o-mini", true, null));

        StockAnalysisResult result = stockAnalysisService.analyze(command);

        assertThat(result.isinCode()).isEqualTo("005930");
        assertThat(result.trendStatus()).isEqualTo(TrendStatus.REGULAR);
        assertThat(result.report()).isSameAs(report);
        assertThat(result.analyzedAt()).isNotNull();
        verify(technicalDataPort).loadTechnicalContext("005930");
        verify(llmClientPort).generateInsight(anyString(), ArgumentMatchers.eq(context));
        ArgumentCaptor<StockAiAnalysisRecord> saved = ArgumentCaptor.forClass(StockAiAnalysisRecord.class);
        verify(stockAiAnalysisPort).save(saved.capture());
        assertThat(saved.getValue().stockId()).isEqualTo(42L);
        assertThat(saved.getValue().tradeDate()).isEqualTo(input.tradeDate());
        assertThat(saved.getValue().inputManifest()).isEqualTo(input.inputManifest());
        assertThat(saved.getValue().status()).isEqualTo("SUCCEEDED");
        assertThat(saved.getValue().modelVersion()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("LLM 호출 시 서비스의 시스템 지시문과 기술 컨텍스트를 함께 전달한다")
    void analyze_PassesSystemInstructionAndContext() {
        StockAnalysisCommand command = new StockAnalysisCommand("000660");
        CanonicalStockAnalysisInput input = createInput("000660", TrendStatus.NEUTRAL);
        AiAnalysisContext context = input.context();
        AiReport report = new AiReport(InvestmentDecision.HOLD, 50, "중립 흐름", List.of("reason"), "분석 결과");
        given(technicalDataPort.loadTechnicalContext("000660")).willReturn(input);
        given(llmClientPort.generateInsight(anyString(), ArgumentMatchers.eq(context)))
                .willReturn(new LlmClientPort.AiGenerationResult(report, "gpt-4o-mini", true, null));

        stockAnalysisService.analyze(command);

        ArgumentCaptor<String> instructionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClientPort).generateInsight(instructionCaptor.capture(), ArgumentMatchers.eq(context));
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
        verify(stockAiAnalysisPort, never()).save(any());
    }

    @Test
    @DisplayName("LLM 분석 실패 시 예외를 전파한다")
    void analyze_PropagatesLlmFailure() {
        StockAnalysisCommand command = new StockAnalysisCommand("005930");
        CanonicalStockAnalysisInput input = createInput("005930", TrendStatus.INVERSE);
        AiAnalysisContext context = input.context();
        RuntimeException exception = new IllegalStateException("LLM 장애");
        given(technicalDataPort.loadTechnicalContext("005930")).willReturn(input);
        given(llmClientPort.generateInsight(anyString(), ArgumentMatchers.eq(context))).willThrow(exception);

        assertThatThrownBy(() -> stockAnalysisService.analyze(command))
                .isSameAs(exception);
        verify(stockAiAnalysisPort, never()).save(any());
    }

    @Test
    @DisplayName("LLM 복구 결과는 실패 상태로 canonical AI 테이블에 기록한다")
    void analyze_persistsLlmFallbackAsFailed() {
        CanonicalStockAnalysisInput input = createInput("005930", TrendStatus.NEUTRAL);
        given(technicalDataPort.loadTechnicalContext("005930")).willReturn(input);
        given(llmClientPort.generateInsight(anyString(), ArgumentMatchers.eq(input.context())))
                .willReturn(new LlmClientPort.AiGenerationResult(AiReport.fallback(), "gpt-4o-mini", false,
                        "AI_GENERATION_FAILED"));

        stockAnalysisService.analyze(new StockAnalysisCommand("005930"));

        ArgumentCaptor<StockAiAnalysisRecord> saved = ArgumentCaptor.forClass(StockAiAnalysisRecord.class);
        verify(stockAiAnalysisPort).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("fallback 판정용 내부 메서드는 응답 JSON 필드를 추가하지 않는다")
    void fallbackMarkerDoesNotChangeAiReportJsonContract() {
        var json = new ObjectMapper().valueToTree(AiReport.fallback());

        assertThat(json.has("fallback")).isFalse();
    }

    private CanonicalStockAnalysisInput createInput(String isinCode, TrendStatus trendStatus) {
        LocalDate tradeDate = LocalDate.of(2026, 4, 24);
        AiAnalysisContext context = new AiAnalysisContext(
                isinCode,
                tradeDate,
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
        return new CanonicalStockAnalysisInput(42L, isinCode, "테스트 종목", tradeDate, "KRX", 7,
                "{\"priceRevision\":7}", "test-v1", context);
    }
}
