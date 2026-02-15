package org.stockwellness.application.port.in.stock.command;

import org.stockwellness.domain.stock.exception.InvalidStockCodeException;

public record StockAnalysisCommand(
    String isinCode
) {
    // Compact Constructor를 활용한 유효성 검증
    public StockAnalysisCommand {
        if (isinCode == null || isinCode.isBlank()) {
            throw new InvalidStockCodeException("종목 코드는 필수입니다.");
        }

        if (!isinCode.matches("^[0-9A-Za-z]{4,12}$")) {
            throw new InvalidStockCodeException("올바른 종목 코드 형식이 아닙니다.");
        }
    }
}