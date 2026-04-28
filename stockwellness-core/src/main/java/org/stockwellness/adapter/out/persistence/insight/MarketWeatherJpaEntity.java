package org.stockwellness.adapter.out.persistence.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.stockwellness.domain.shared.AbstractEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "market_weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketWeatherJpaEntity extends AbstractEntity {

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(nullable = false, length = 20)
    private String marketType;

    @Column(nullable = false)
    private int weatherScore;

    @Column(nullable = false, length = 20)
    private String weatherState;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String topSectors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String bottomSectors;

    @Builder
    public MarketWeatherJpaEntity(LocalDate baseDate, String marketType, int weatherScore, String weatherState, String aiSummary, String topSectors, String bottomSectors) {
        this.baseDate = baseDate;
        this.marketType = marketType;
        this.weatherScore = weatherScore;
        this.weatherState = weatherState;
        this.aiSummary = aiSummary;
        this.topSectors = topSectors;
        this.bottomSectors = bottomSectors;
    }
}
