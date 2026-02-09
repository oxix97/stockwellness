package org.stockwellness.application.port.in.stock.result;

import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.time.LocalDateTime;

/**
 * AI 분석 결과를 담는 불변 객체 (Output DTO)
 * - Service -> Controller, Service -> Redis 흐름에서 사용됩니다.
 */
public record StockAnalysisResult(
        String isinCode,              // 종목 코드
        TrendStatus trendStatus,    // 추세 상태 (UI 뱃지/색상 표시용)
        AiReport report,       // AI가 생성한 분석 텍스트 전문
        LocalDateTime analyzedAt    // 분석 실행 시점 (데이터의 신선도 표시용)
) {
    
    // 편의를 위한 추가 생성자 (Service에서 호출 시 시간 자동 주입)
    public StockAnalysisResult(String isinCode, TrendStatus trendStatus, AiReport report) {
        this(isinCode, trendStatus, report, LocalDateTime.now());
    }

    // 캐싱된 데이터인지 확인하기 위한 Helper 메서드 예시
    public boolean isExpired(int hoursValid) {
        return analyzedAt.plusHours(hoursValid).isBefore(LocalDateTime.now());
    }
}