package org.stockwellness.domain.portfolio.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 백테스트 시 현금 흐름(입금)을 정의하는 인터페이스
 */
public interface CashFlowModel {
    /**
     * 해당 날짜에 추가로 투입할 금액을 반환합니다.
     */
    BigDecimal getInvestmentAmount(LocalDate date, boolean isFirstDay);

    /**
     * 초기 투입 금액을 반환합니다.
     */
    BigDecimal getInitialAmount();
}
