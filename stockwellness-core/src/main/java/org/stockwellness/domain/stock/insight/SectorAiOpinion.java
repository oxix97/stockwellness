package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class SectorAiOpinion {

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_decision", length = 20)
    private InvestmentDecision decision; // BUY, SELL, HOLD

    @Column(name = "ai_confidence_score")
    private int confidenceScore; // 0 ~ 100

    @Column(name = "ai_title", length = 500)
    private String title; // 한 줄 요약 (헤드라인)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_key_reasons", columnDefinition = "jsonb")
    private List<String> keyReasons = new ArrayList<>(); // 핵심 근거 3가지

    @Column(name = "ai_detailed_analysis", columnDefinition = "text")
    private String detailedAnalysis; // 상세 리포트 본문

    public static SectorAiOpinion of(
            InvestmentDecision decision,
            int confidenceScore,
            String title,
            List<String> keyReasons,
            String detailedAnalysis
    ) {
        return new SectorAiOpinion(
                decision,
                confidenceScore,
                title,
                keyReasons != null ? new ArrayList<>(keyReasons) : new ArrayList<>(),
                detailedAnalysis
        );
    }

    public static SectorAiOpinion empty() {
        return new SectorAiOpinion(null, 0, null, new ArrayList<>(), null);
    }
}
