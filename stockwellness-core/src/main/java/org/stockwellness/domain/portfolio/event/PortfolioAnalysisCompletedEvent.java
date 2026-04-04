package org.stockwellness.domain.portfolio.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioAnalysisCompletedEvent(
    Long portfolioId,
    LocalDate baseDate,
    BigDecimal mdd,
    BigDecimal sharpeRatio,
    BigDecimal beta
) {}
