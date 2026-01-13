package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.external.ai.AiAnalysisContext;
import org.stockwellness.adapter.out.external.ai.PromptTemplateMapper;
import org.stockwellness.application.port.in.GetStockAnalysisUseCase;
import org.stockwellness.application.port.out.LlmClientPort;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetStockAnalysisService implements GetStockAnalysisUseCase {

    private final PromptContextService promptContextService; // 1. 데이터 준비
    private final PromptTemplateMapper promptTemplateMapper; // 2. 프롬프트 포맷팅
    private final LlmClientPort llmClientPort;             // 3. AI 호출 (Output Port)

    // 시스템 프롬프트 (AI의 페르소나 정의)
    private static final String SYSTEM_INSTRUCTION = """
            당신은 Stockwellness의 AI 파트너입니다.
            제공된 기술적 지표(RSI, MACD, 추세)를 바탕으로 개인 투자자에게
            객관적이고 전문적인 한국어 분석 리포트를 작성하십시오.
            '~입니다', '~합니다' 체를 사용하고, 감정적인 표현은 배제하십시오.
            """;

    private static final String DISCLAIMER = "\n\n* 본 분석은 과거 데이터를 기반으로 한 시뮬레이션 결과이며, 실제 투자의 책임은 투자자 본인에게 있습니다.";

    @Override
    @Cacheable(value = "ai_analysis", key = "#command.isinCode()", unless = "#result == null")
    public StockAnalysisResult analyze(StockAnalysisCommand command) {
        log.info("Analyzing stock: {}", command.isinCode());

        // 1. Context 조회 (DB + Domain Logic)
        // 배치로 계산된 ma60, ma120 등의 지표가 포함된 데이터를 가져옵니다.
        AiAnalysisContext context = promptContextService.getContext(command.isinCode());

        // 2. Prompt 생성 (Adapter)
        // "RSI 상태: OVERBOUGHT" 등의 텍스트로 변환합니다.
        String userContext = promptTemplateMapper.toPromptString(context);

        // 3. AI 호출 (External API)
        // Virtual Thread가 적용된 RestClient를 통해 호출됩니다.
        String aiRawResponse = llmClientPort.generateInsight(SYSTEM_INSTRUCTION, userContext);

        // 4. 결과 조합 (후처리)
        // 면책 조항을 강제로 붙여서 리스크를 관리합니다.
        String finalResponse = aiRawResponse + DISCLAIMER;

        return new StockAnalysisResult(context.isinCode(), finalResponse);
    }
}