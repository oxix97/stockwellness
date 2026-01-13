package org.stockwellness.adapter.out.external.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PromptTemplateMapper {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        [자산 분석 보고서]
        종목코드: %s (%s 기준)
        현재가: %s (전일대비 %s%%)
        
        [기술적 지표 요약]
        - 추세(단기): %s
        - RSI 상태: %s
        - MACD 신호: %s
        
        위 데이터를 바탕으로 투자자에게 객관적인 상황을 브리핑해줘.
        """;

    public String toPromptString(AiAnalysisContext context) {
        String priceStr = formatCurrency(context.price().closePrice());
        String rateStr = formatRate(context.price().fluctuationRate());

        return SYSTEM_PROMPT_TEMPLATE.formatted(
                context.isinCode(),
                context.analysisDate(),
                priceStr,
                rateStr,
                context.technical().trend(),
                context.technical().rsiStatus(),
                context.technical().macdSignal()
        );
    }

    private String formatCurrency(BigDecimal price) {
        if (price == null) return "0";
        if (price.compareTo(new BigDecimal("1000")) < 0) {
            return String.format("%,.2f", price);
        }
        return String.format("%,.0f", price);
    }

    private String formatRate(BigDecimal rate) {
        // null safe 처리 및 소수점 포맷팅
        if (rate == null) return "0.00";
        return String.format("%.2f", rate);
    }
}