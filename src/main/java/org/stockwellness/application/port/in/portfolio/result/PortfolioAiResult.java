package org.stockwellness.application.port.in.portfolio.result;

import java.util.List;

public record PortfolioAiResult(
        String summary,    // e.g., "Growth Archer"
        String insight,    // Overall commentary
        List<String> nextSteps // Actionable advice
) {
}
