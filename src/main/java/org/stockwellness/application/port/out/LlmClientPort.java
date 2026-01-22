package org.stockwellness.application.port.out;

import org.stockwellness.adapter.out.external.ai.dto.AiReport;

public interface LlmClientPort {
    /**
     * AI에게 분석 요청을 보내고 답변을 받습니다.
     * @param systemInstruction AI의 역할과 규칙 (System Prompt)
     * @param userContext 분석할 데이터 컨텍스트 (User Prompt)
     * @return AI의 답변 텍스트
     */
    AiReport generateInsight(String systemInstruction, String userContext);
}
