package org.stockwellness.domain.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "portfolio_stats")
@ToString
@NoArgsConstructor(access = PROTECTED)
public class PortfolioStats {

    @Id
    @Column(name = "portfolio_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "window_start")
    private LocalDate windowStart;

    @Column(name = "price_basis", nullable = false, length = 30)
    private String priceBasis;

    @Column(name = "configuration_version", nullable = false)
    private long configurationVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_manifest", nullable = false)
    private Map<String, Object> inputManifest;

    @Column(name = "mdd", precision = 12, scale = 6)
    private BigDecimal mdd;

    @Column(name = "sharpe_ratio", precision = 12, scale = 6)
    private BigDecimal sharpeRatio;

    @Column(name = "beta", precision = 12, scale = 6)
    private BigDecimal beta;

    @Column(name = "inception_return", precision = 19, scale = 4)
    private BigDecimal inceptionReturn;

    @Column(name = "benchmark_return", precision = 19, scale = 4)
    private BigDecimal benchmarkReturn;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public static PortfolioStats create(Portfolio portfolio, LocalDate windowStart, LocalDate baseDate,
                                        String priceBasis, Map<String, Object> inputManifest,
                                        BigDecimal mdd, BigDecimal sharpeRatio, BigDecimal beta,
                                        BigDecimal inceptionReturn, BigDecimal benchmarkReturn) {
        var stats = new PortfolioStats();
        stats.portfolio = portfolio;
        stats.update(portfolio.getConfigurationVersion(), windowStart, baseDate, priceBasis, inputManifest, mdd, sharpeRatio, beta,
                inceptionReturn, benchmarkReturn);
        return stats;
    }

    public void update(long configurationVersion, LocalDate windowStart, LocalDate baseDate, String priceBasis,
                       Map<String, Object> inputManifest, BigDecimal mdd, BigDecimal sharpeRatio,
                       BigDecimal beta, BigDecimal inceptionReturn, BigDecimal benchmarkReturn) {
        this.baseDate = baseDate;
        this.windowStart = windowStart;
        this.priceBasis = priceBasis;
        this.configurationVersion = configurationVersion;
        this.inputManifest = Map.copyOf(inputManifest);
        this.mdd = mdd;
        this.sharpeRatio = sharpeRatio;
        this.beta = beta;
        this.inceptionReturn = inceptionReturn;
        this.benchmarkReturn = benchmarkReturn;
        this.calculatedAt = Instant.now();
    }
}
