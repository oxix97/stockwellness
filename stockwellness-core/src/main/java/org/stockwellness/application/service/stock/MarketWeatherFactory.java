package org.stockwellness.application.service.stock;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;

import java.time.LocalDate;

@Component
public class MarketWeatherFactory {

    public MarketWeatherResult create(MarketWeatherLevel level, MarketWeatherReason reason, LocalDate asOfDate) {
        return new MarketWeatherResult(
                level,
                level.headline(),
                descriptionFor(reason, level),
                reason,
                asOfDate
        );
    }

    private String descriptionFor(MarketWeatherReason reason, MarketWeatherLevel level) {
        return switch (reason) {
            case BROAD_RALLY -> "대형주와 중소형주가 함께 오르며 시장 전반에 온기가 퍼지고 있어요";
            case STEADY_ADVANCE -> "주요 지수가 고르게 오르며 투자심리가 비교적 안정적인 편이에요";
            case NARROW_ADVANCE -> "지수는 오르고 있지만 종목별 분위기는 조금 엇갈리고 있어요";
            case SIDEWAYS -> "시장이 뚜렷한 방향 없이 관망 흐름을 보이고 있어요";
            case HIDDEN_WEAKNESS -> "지수는 버티고 있지만 하락 종목이 더 많아 체감은 무거운 편이에요";
            case BROAD_SELL_OFF -> "시장 전반에 매도 압력이 퍼지며 투자심리가 위축되고 있어요";
            case VOLATILE_SELL_OFF -> "낙폭이 커지고 변동성도 확대돼 방어적으로 볼 필요가 있는 장이에요";
            case INDEX_ONLY_RALLY -> "주요 지수가 함께 오르며 시장 전반의 분위기가 비교적 밝은 편이에요";
            case INDEX_ONLY_ADVANCE -> "주요 지수 흐름을 기준으로 보면 투자심리가 비교적 안정적인 편이에요";
            case INDEX_ONLY_MIXED -> "주요 지수는 버티고 있지만 시장 전체 분위기는 조금 더 지켜볼 필요가 있어요";
            case INDEX_ONLY_WEAKNESS -> "주요 지수 흐름을 기준으로 보면 시장 심리가 다소 약해진 모습이에요";
            case INDEX_ONLY_STORM -> "주요 지수 낙폭이 커서 방어적으로 볼 필요가 있는 장이에요";
            case INDEX_ONLY_SIDEWAYS -> "주요 지수가 보합권에 머물며 뚜렷한 방향성이 보이지 않아요";
        };
    }
}
