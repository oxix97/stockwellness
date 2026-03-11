package org.stockwellness.domain.portfolio.advisor;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.shared.AbstractEntity;

/**
 * AI 어드바이저 분석 보고서 엔티티
 */
@Entity
@Getter
@Table(name = "advisor_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvisorReport extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdviceAction action;

    private AdvisorReport(Portfolio portfolio, String content, AdviceAction action) {
        this.portfolio = portfolio;
        this.content = content;
        this.action = action;
    }

    public static AdvisorReport create(Portfolio portfolio, String content, AdviceAction action) {
        return new AdvisorReport(portfolio, content, action);
    }
}
