package org.stockwellness.adapter.in.web.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 응답 DTO
 * - 도메인 객체(StockAnalysisResult)를 프론트엔드 친화적인 JSON 구조로 변환합니다.
 */
public record StockAnalysisResponse(
        @Schema(description = "종목 코드", example = "005930")
        String isinCode,

        @Schema(description = "기술적 추세 상태 (REGULAR, INVERSE, NEUTRAL)", example = "REGULAR")
        String trendStatus,

        @Schema(description = "추세 상태 설명", example = "정배열 (상승추세)")
        String trendStatusDescription,

        @Schema(description = "추세 상태 색상 코드", example = "#F44336")
        String trendStatusColor,

        @Schema(description = "AI 구조화된 분석 리포트")
        AiAnalysisDetail aiResult,

        @Schema(description = "데이터 기준 일시", example = "2024-05-21T15:30:00")
        LocalDateTime generatedAt
) {

    // Factory Method: 도메인 객체 -> API 응답 DTO 변환
    public static StockAnalysisResponse from(StockAnalysisResult result) {
        return new StockAnalysisResponse(
                result.isinCode(),
                result.trendStatus() != null ? result.trendStatus().name() : "UNKNOWN",
                result.trendStatus() != null ? result.trendStatus().getDescription() : "알 수 없음",
                result.trendStatus() != null ? result.trendStatus().getColorCode() : "#9E9E9E",
                AiAnalysisDetail.from(result.report()), // 도메인 객체 매핑
                result.analyzedAt()
        );
    }

    // [Inner Record] AI 분석 상세 데이터
    public record AiAnalysisDetail(
            @Schema(description = "투자 의견 (BUY, SELL, HOLD)", example = "BUY")
            String decision,

            @Schema(description = "투자 의견 한글 라벨", example = "매수")
            String decisionLabel,

            @Schema(description = "AI 확신도 (0~100)", example = "85")
            int confidenceScore,

            @Schema(description = "한 줄 요약 제목", example = "골든크로스 발생, 강력 매수 기회")
            String title,

            @Schema(description = "핵심 근거 3가지")
            List<String> keyReasons,

            @Schema(description = "상세 분석 내용 (Markdown 포맷)", example = "현재 주가는 20일 이동평균선을...")
            String detailedAnalysis
    ) {
        public static AiAnalysisDetail from(AiReport report) {
            if (report == null) return null;
            return new AiAnalysisDetail(
                    report.decision() != null ? report.decision().name() : "HOLD",
                    report.decision() != null ? report.decision().getLabel() : "관망",
                    report.confidenceScore(),
                    report.title(),
                    report.keyReasons(),
                    report.detailedAnalysis()
            );
        }
    }
}