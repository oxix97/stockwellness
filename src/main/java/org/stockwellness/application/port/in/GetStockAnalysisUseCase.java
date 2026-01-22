package org.stockwellness.application.port.in;

import org.stockwellness.application.port.StockAnalysisCommand;
import org.stockwellness.application.port.StockAnalysisResult;

public interface GetStockAnalysisUseCase {
    /**
     * 특정 종목에 대한 AI 분석 리포트를 생성합니다.
     * @param command 분석 요청 정보 (종목코드, 사용자ID 등)
     * @return 분석 결과 (종목명, AI 답변)
     */
    StockAnalysisResult analyze(StockAnalysisCommand command);
}