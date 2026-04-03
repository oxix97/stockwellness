package org.stockwellness.domain.portfolio.advisor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.shared.AbstractEntity;

/**
 * AI 어드바이저가 특정 포트폴리오를 분석하여 생성한 조언 리포트 엔티티입니다.
 * 분석 내용(content)과 분석 결과에 따른 권장 조치(AdviceAction)를 포함합니다.
 */
@Entity
@Getter
@Table(name = "advisor_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvisorReport extends AbstractEntity {

    /**
     * 리포트의 대상이 되는 포트폴리오
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /**
     * AI가 생성한 상세 분석 내용 (Markdown 형식 등 텍스트 데이터)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 분석 결과에 따른 권장 조치 타입 (매수 유지, 비중 축소, 리밸런싱 필요 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdviceAction action;

    private AdvisorReport(Portfolio portfolio, String content, AdviceAction action) {
        this.portfolio = portfolio;
        this.content = content;
        this.action = action;
    }

    /**
     * 새로운 AI 조언 리포트 인스턴스를 생성합니다.
     *
     * @param portfolio 대상 포트폴리오
     * @param content 분석 내용
     * @param action 권장 조치
     * @return 생성된 AdvisorReport 객체
     */
    public static AdvisorReport create(Portfolio portfolio, String content, AdviceAction action) {
        return new AdvisorReport(portfolio, content, action);
    }
}
