package org.stockwellness.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockStatus {

    ACTIVE("정상 거래", true),          // 정상
    HALTED("거래 정지", false),         // 일시적 거래 정지 (조회는 가능하나 매매 불가)
    ADMINISTRATIVE("관리 종목", true),  // 관리 종목 (거래는 가능하나 위험)
    DELISTED("상장 폐지", false);       // 상장 폐지 (더 이상 데이터 수집 안 함)

    private final String description;
    private final boolean isTradable;

    // 상장 폐지된 종목만 수집 대상에서 제외
    public boolean isCollectingTarget() {
        return this != DELISTED;
    }
}