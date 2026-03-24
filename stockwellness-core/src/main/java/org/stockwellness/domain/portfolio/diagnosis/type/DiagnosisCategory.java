package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 건강 진단 레이더 차트의 5대 핵심 지표 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum DiagnosisCategory {
    RETURN("return", "수익"),           // 수익성
    STABILITY("stability", "안전"),     // 변동성 및 MDD 관리
    DIVERSIFICATION("diversification", "분산"), // 자산/섹터 분산
    AGILITY("agility", "민첩"),         // 시장 변화 대응력 (베타)
    CASH("cash", "현금");               // 유동성 비중

    private final String key;
    private final String label;
}
