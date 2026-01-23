package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.application.port.in.stock.StockAnalysisCommand;
import org.stockwellness.application.port.out.stock.StockAnalysisResult;
import org.stockwellness.application.port.in.stock.StockAnalysisUseCase;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisService implements StockAnalysisUseCase {

    private final LoadTechnicalDataPort technicalDataPort;
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

        // 1. 도메인 데이터 로드 (Port -> Domain DTO)
        AiAnalysisContext context = technicalDataPort.loadTechnicalContext(command.isinCode());

        // 2. AI 분석 요청 (String 변환 책임은 Adapter로 위임됨)
        AiReport report = llmClientPort.generateInsight(SYSTEM_INSTRUCTION, context);

        return new StockAnalysisResult(
                context.isinCode(),
                context.technicalSignal().trendStatus(),
                report
        );
    }
}