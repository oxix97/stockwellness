package org.stockwellness.domain.stock.analysis;

/**
 * 기술적 점수 산출을 위한 개별 정책 인터페이스
 */
public interface ScoringPolicy {
    /**
     * 특정 지표 스냅샷을 기반으로 가중치 점수를 반환합니다.
     */
    int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot);
}
