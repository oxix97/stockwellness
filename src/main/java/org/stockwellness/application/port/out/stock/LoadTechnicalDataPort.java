package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

import java.util.List;
import java.util.Map;

public interface LoadTechnicalDataPort {
    /**
     * 종목 코드에 해당하는 AI 분석용 기술적 데이터 컨텍스트를 로드합니다.
     */
    AiAnalysisContext loadTechnicalContext(String isinCode);

    /**
     * 여러 종목 코드에 해당하는 AI 분석용 기술적 데이터 컨텍스트를 배치로 로드합니다.
     */
    Map<String, AiAnalysisContext> loadTechnicalContexts(List<String> isinCodes);
}