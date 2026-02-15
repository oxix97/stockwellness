package org.stockwellness.domain.stock.analysis;

import lombok.Getter;

@Getter
public enum InvestmentDecision {
    BUY("매수", "기술적 지표가 상승 신호를 보이며 적극적인 진입이 고려됩니다."),
    SELL("매도", "하락 추세가 뚜렷하며 리스크 관리를 위한 매도를 권장합니다."),
    HOLD("관망", "추세가 불분명하여 추가적인 시장 관찰이 필요한 시점입니다.");

    private final String label;
    private final String description;

    InvestmentDecision(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
