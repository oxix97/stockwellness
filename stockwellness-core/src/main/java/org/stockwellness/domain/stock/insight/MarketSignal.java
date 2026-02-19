package org.stockwellness.domain.stock.insight;

import org.stockwellness.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "market_signal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketSignal extends AbstractEntity {

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Enumerated(STRING)
    @Column(nullable = false, length = 30)
    private SignalType signalType;

    @Column(precision = 10, scale = 2)
    private BigDecimal intensity; // 강도 (예: 5.0배)

    // --- AI Analysis Section ---
    private boolean isAnalyzed;

    @Column(columnDefinition = "TEXT")
    private String aiSummary; // AI가 생성한 요약

    private LocalDateTime analyzedAt;

    public static MarketSignal of(Stock stock, LocalDate baseDate, SignalType signalType, BigDecimal intensity) {
        MarketSignal marketSignal = new MarketSignal();
        marketSignal.stock = stock;
        marketSignal.baseDate = baseDate;
        marketSignal.signalType = signalType;
        marketSignal.intensity = intensity;
        marketSignal.isAnalyzed = false;
        return marketSignal;
    }

    public void updateAiAnalysis(String summary) {
        this.aiSummary = summary;
        this.isAnalyzed = true;
        this.analyzedAt = LocalDateTime.now();
    }
}