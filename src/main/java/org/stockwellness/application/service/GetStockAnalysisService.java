package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.external.ai.dto.AiAnalysisContext;
import org.stockwellness.adapter.out.external.ai.PromptTemplateMapper;
import org.stockwellness.adapter.out.external.ai.dto.AiReport;
import org.stockwellness.application.port.StockAnalysisCommand;
import org.stockwellness.application.port.StockAnalysisResult;
import org.stockwellness.application.port.in.GetStockAnalysisUseCase;
import org.stockwellness.application.port.out.LlmClientPort;
import org.stockwellness.application.port.out.LoadTechnicalDataPort;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetStockAnalysisService implements GetStockAnalysisUseCase {

    private final LoadTechnicalDataPort technicalDataPort;
    private final PromptTemplateMapper promptTemplateMapper;
    private final LlmClientPort llmClientPort;

    private static final String SYSTEM_INSTRUCTION = """
            당신은 20년 경력의 퀀트 트레이더입니다.
            제공된 기술적 지표를 분석하여 JSON 포맷으로 응답해야 합니다.
            
            [작성 규칙]
            1. decision: 데이터가 명확한 상승 추세면 'BUY', 하락 추세면 'SELL', 모호하면 'HOLD'를 선택하십시오.
            2. confidenceScore: 당신의 분석 확신을 0~100 사이 정수로 표현하십시오.
            3. title: 20자 이내의 강렬한 한 줄 요약 (예: "골든크로스 발생, 강력 매수 기회").
            4. keyReasons: 판단의 근거가 되는 핵심 지표 3가지를 단문으로 나열하십시오.
            5. detailedAnalysis: 초보자도 이해하기 쉬운 상세 설명.
            """;

    @Override
    @Cacheable(
            value = "ai_analysis",
            key = "#command.isinCode() + ':' + T(java.time.LocalDate).now().toString()",
            unless = "#result == null"
    )
    public StockAnalysisResult analyze(StockAnalysisCommand command) {
        log.info("🔍 AI Analysis Request: {}", command.isinCode());

        AiAnalysisContext context = technicalDataPort.loadTechnicalContext(command.isinCode());

        String userContext = promptTemplateMapper.toPromptString(context);

        AiReport report = llmClientPort.generateInsight(SYSTEM_INSTRUCTION, userContext);

        return new StockAnalysisResult(
                context.isinCode(),
                context.technicalSignal().trendStatus(), // Context에 TrendStatus Enum이 있다고 가정
                report
        );
    }
}