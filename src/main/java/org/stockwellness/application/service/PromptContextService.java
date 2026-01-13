package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryJpaRepository;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.adapter.out.external.ai.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.MacdSignal;
import org.stockwellness.domain.stock.analysis.RsiSignal;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptContextService {

    private final StockHistoryJpaRepository stockHistoryRepository;

    public AiAnalysisContext getContext(String isinCode) {
        // 최근 2일치 데이터 조회 (전일 대비 비교가 필요할 경우를 대비해 2건 조회)
        List<StockHistory> histories = stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(isinCode);

        if (histories.isEmpty()) {
            throw new IllegalArgumentException("데이터가 부족하여 분석할 수 없습니다. Code: " + isinCode);
        }

        StockHistory today = histories.get(0);
        RsiSignal rsiSignal = RsiSignal.analyze(today.getRsi14());
        MacdSignal macdSignal = MacdSignal.analyze(today.getMacd());

        // 2. DTO 생성
        return new AiAnalysisContext(
                today.getIsinCode(),
                today.getBaseDate(),
                new AiAnalysisContext.PriceSummary(
                        today.getClosePrice(),
                        today.getFluctuationRate() // [수정] 직접 계산 대신 엔티티 필드 사용
                ),
                new AiAnalysisContext.TechnicalSignal(
                        analyzeTrend(today),
                        rsiSignal.name(),
                        macdSignal.getDescription(),
                        0.0
                ),
                new AiAnalysisContext.PortfolioRisk(false, 0.0)
        );
    }

    private String analyzeTrend(StockHistory history) {
        if (history.getMa20() == null || history.getMa60() == null) return "UNKNOWN";
        return history.getMa20().compareTo(history.getMa60()) > 0 ? "UPWARD" : "DOWNWARD";
    }
}