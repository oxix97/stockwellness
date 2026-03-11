package org.stockwellness.domain.portfolio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@ToString
@NoArgsConstructor(access = PROTECTED)
public class PortfolioStats extends AbstractEntity {

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(precision = 7, scale = 4)
    private BigDecimal mdd;

    @Column(precision = 7, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(precision = 7, scale = 4)
    private BigDecimal beta;

    public static PortfolioStats create(Portfolio portfolio, LocalDate baseDate, BigDecimal mdd, BigDecimal sharpeRatio, BigDecimal beta) {
        PortfolioStats stats = new PortfolioStats();
        stats.portfolio = portfolio;
        stats.baseDate = baseDate;
        stats.mdd = mdd;
        stats.sharpeRatio = sharpeRatio;
        stats.beta = beta;
        return stats;
    }

    public void update(LocalDate baseDate, BigDecimal mdd, BigDecimal sharpeRatio, BigDecimal beta) {
        this.baseDate = baseDate;
        this.mdd = mdd;
        this.sharpeRatio = sharpeRatio;
        this.beta = beta;
    }
}
