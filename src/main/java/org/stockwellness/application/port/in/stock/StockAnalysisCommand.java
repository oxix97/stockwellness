package org.stockwellness.application.port.in.stock;

import org.stockwellness.domain.stock.exception.InvalidStockCodeException;

public record StockAnalysisCommand(
    String isinCode
) {
    // Compact Constructor를 활용한 유효성 검증
    public StockAnalysisCommand {
        if (isinCode == null || isinCode.isBlank()) {
            // BusinessException은 프로젝트 공통 예외 클래스라고 가정합니다.
            throw new InvalidStockCodeException("종목 코드는 필수입니다.");
        }
        
        // 포맷 검증 (예: 숫자+영문 6자리 이상)
        // Controller의 @Valid를 통과했더라도 도메인 레벨에서 한 번 더 방어하는 것이 안전합니다.
        if (isinCode.length() < 4) {
             throw new InvalidStockCodeException("유효하지 않은 종목 코드입니다.");
        }
    }
}