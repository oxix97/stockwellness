package org.stockwellness.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.shared.AbstractEntity;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

/**
 * 포트폴리오의 주요 통계 및 위험 지표를 관리하는 도메인 모델입니다.
 * 최대 낙폭(MDD), 샤프 지수, 베타 지수 등의 지표를 포함합니다.
 */
@Getter
@Entity
@ToString
@NoArgsConstructor(access = PROTECTED)
public class PortfolioStats extends AbstractEntity {

    /**
     * 해당 통계의 대상 포트폴리오
     */
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /**
     * 통계 계산 기준일
     */
    @Column(nullable = false)
    private LocalDate baseDate;

    /**
     * 최대 낙폭 (MDD, Maximum Drawdown)
     * 특정 기간 동안 고점 대비 저점의 최대 하락률을 나타냅니다.
     */
    @Column(precision = 7, scale = 4)
    private BigDecimal mdd;

    /**
     * 샤프 지수 (Sharpe Ratio)
     * 위험 대비 수익률을 나타내며, 지수가 높을수록 위험 대비 성과가 우수함을 의미합니다.
     */
    @Column(precision = 7, scale = 4)
    private BigDecimal sharpeRatio;

    /**
     * 베타 지수 (Beta)
     * 시장 지수(KOSPI 등) 대비 변동성을 나타냅니다. 1보다 크면 시장보다 변동성이 큽니다.
     */
    @Column(precision = 7, scale = 4)
    private BigDecimal beta;

    /**
     * 설정 시점 이후 누적 수익률
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal inceptionReturn;

    /**
     * 비교 벤치마크 수익률
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal benchmarkReturn;

    /**
     * 새로운 포트폴리오 통계 인스턴스를 생성합니다.
     *
     * @param portfolio 대상 포트폴리오
     * @param baseDate 기준일
     * @param mdd 최대 낙폭
     * @param sharpeRatio 샤프 지수
     * @param beta 베타 지수
     * @param inceptionReturn 설정 이후 수익률
     * @param benchmarkReturn 벤치마크 수익률
     * @return 생성된 PortfolioStats 객체
     */
    public static PortfolioStats create(Portfolio portfolio, LocalDate baseDate, BigDecimal mdd, BigDecimal sharpeRatio, BigDecimal beta, BigDecimal inceptionReturn, BigDecimal benchmarkReturn) {
        PortfolioStats stats = new PortfolioStats();
        stats.portfolio = portfolio;
        stats.baseDate = baseDate;
        stats.mdd = mdd;
        stats.sharpeRatio = sharpeRatio;
        stats.beta = beta;
        stats.inceptionReturn = inceptionReturn;
        stats.benchmarkReturn = benchmarkReturn;
        return stats;
    }

    /**
     * 통계 지표를 최신 데이터로 업데이트합니다.
     *
     * @param baseDate 새로운 기준일
     * @param mdd 새로운 최대 낙폭
     * @param sharpeRatio 새로운 샤프 지수
     * @param beta 새로운 베타 지수
     * @param inceptionReturn 새로운 설정 이후 수익률
     * @param benchmarkReturn 새로운 벤치마크 수익률
     */
    public void update(LocalDate baseDate, BigDecimal mdd, BigDecimal sharpeRatio, BigDecimal beta, BigDecimal inceptionReturn, BigDecimal benchmarkReturn) {
        this.baseDate = baseDate;
        this.mdd = mdd;
        this.sharpeRatio = sharpeRatio;
        this.beta = beta;
        this.inceptionReturn = inceptionReturn;
        this.benchmarkReturn = benchmarkReturn;
    }
}
