package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.analysis.MarketCondition;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTechnicalDataAdapter implements LoadTechnicalDataPort {

    private final StockHistoryRepository stockHistoryRepository;

    @Override
    public AiAnalysisContext loadTechnicalContext(String isinCode) {
        // 1. DB 조회
        List<StockHistory> histories = stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(isinCode);

        if (histories.isEmpty()) {
            throw new IllegalArgumentException("데이터가 부족하여 분석할 수 없습니다. Code: " + isinCode);
        }

        StockHistory today = histories.get(0);
        StockHistory yesterday = histories.size() > 1 ? histories.get(1) : null;

        // 2. 도메인 로직을 통한 정밀 분석 (직접 if문 작성 금지)
        // BigDecimal 변환 로직이 필요하다면 여기서 수행
        MarketCondition condition = TechnicalCalculator.analyze(
            today.getMa5(), today.getMa20(), today.getMa60(), today.getMa120(),
            yesterday != null ? yesterday.getMa5() : null,
            yesterday != null ? yesterday.getMa20() : null
        );

        return new AiAnalysisContext(
                today.getIsinCode(),
                today.getBaseDate(),
                // 1. PriceSummary
                new AiAnalysisContext.PriceSummary(
                        today.getClosePrice(),
                        today.getFluctuationRate(),
                        new BigDecimal(today.getVolume())
                ),
                // 2. TechnicalSignal
                new AiAnalysisContext.TechnicalSignal(
                        condition.trendStatus(),        // Enum: REGULAR
                        today.getRsi14(),               // BigDecimal
                        analyzeRsiLevel(today.getRsi14()), // Helper 메서드로 텍스트 변환
                        today.getMacd(),                // BigDecimal
                        condition.signal(),             // Enum: GOLDEN_CROSS
                        today.getMa5(),
                        today.getMa20(),
                        today.getMa60(),
                        today.getMa120()
                ),
                // 3. RiskInfo (일단 기본값)
                new AiAnalysisContext.PortfolioRisk(false, 0.0)
        );
    }

    private String analyzeRsiLevel(BigDecimal rsi) {
        if (rsi == null) return "데이터 없음";
        double val = rsi.doubleValue();
        if (val >= 70) return "과매수(Overbought) - 조정 가능성 높음";
        if (val <= 30) return "과매도(Oversold) - 반등 가능성 높음";
        return "중립(Neutral)";
    }
}