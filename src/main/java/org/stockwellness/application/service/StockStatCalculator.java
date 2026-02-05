package org.stockwellness.application.service;

import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.portfolio.result.StockStatResult;
import org.stockwellness.domain.portfolio.diagnosis.type.AgilityScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.AttackScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.DefenseScorePolicy;
import org.stockwellness.domain.portfolio.diagnosis.type.EnduranceScorePolicy;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.util.List;
import java.util.Objects;

@Service
public class StockStatCalculator {

    public StockStatResult calculate(Stock stock, List<StockHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return buildDefaultStat(stock);
        }

        StockHistory latest = histories.get(0);

        return StockStatResult.of(
                stock.getIsinCode(),
                stock.getName(),
                DefenseScorePolicy.calculate(latest.getMarketCap()),
                AttackScorePolicy.calculate(latest.getRsi14(), latest.getMacd()),
                EnduranceScorePolicy.calculate(latest.getClosePrice(), latest.getMa120()),
                calculateAgility(histories)
        );
    }

    private int calculateAgility(List<StockHistory> histories) {
        double absAvg = histories.stream()
                .limit(5)
                .map(StockHistory::getFluctuationRate)
                .filter(Objects::nonNull)
                .mapToDouble(flt -> Math.abs(flt.doubleValue()))
                .average()
                .orElse(-1.0);

        return AgilityScorePolicy.calculate(absAvg);
    }

    private StockStatResult buildDefaultStat(Stock stock) {
        return StockStatResult.of(
                stock.getIsinCode(),
                stock.getName(),
                50, 50, 50, 50
        );
    }
}