package org.stockwellness.application.port.in;

public interface GetStockAnalysisUseCase {
    /**
     * 특정 종목에 대한 AI 분석 리포트를 생성합니다.
     * @param command 분석 요청 정보 (종목코드, 사용자ID 등)
     * @return 분석 결과 (종목명, AI 답변)
     */
    StockAnalysisResult analyze(StockAnalysisCommand command);

    // 입력 데이터 (Command)
    record StockAnalysisCommand(String isinCode, Long userId) {
        public StockAnalysisCommand {
            if (isinCode == null || isinCode.isBlank()) {
                throw new IllegalArgumentException("종목 코드는 필수입니다.");
            }
        }
    }

    // 출력 데이터 (Result)
    record StockAnalysisResult(String isinCode, String aiResponse) {}
}