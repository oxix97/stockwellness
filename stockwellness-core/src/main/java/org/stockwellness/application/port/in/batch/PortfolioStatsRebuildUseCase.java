package org.stockwellness.application.port.in.batch;

import java.util.List;

public interface PortfolioStatsRebuildUseCase {

    PortfolioStatsRebuildResult rebuild(PortfolioStatsRebuildCommand command);

    record PortfolioStatsRebuildCommand(List<Long> portfolioIds) {
    }

    record PortfolioStatsRebuildResult(int requestedCount) {
    }
}
