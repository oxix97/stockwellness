package org.stockwellness.application.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.batch.PortfolioStatsRebuildUseCase;
import org.stockwellness.application.service.portfolio.PortfolioStatBatchService;

@Service
@RequiredArgsConstructor
public class PortfolioStatsRebuildService implements PortfolioStatsRebuildUseCase {

    private final PortfolioStatBatchService portfolioStatBatchService;

    @Override
    public PortfolioStatsRebuildResult rebuild(PortfolioStatsRebuildCommand command) {
        portfolioStatBatchService.updatePortfolioStatsBatch(command.portfolioIds());
        return new PortfolioStatsRebuildResult(command.portfolioIds().size());
    }
}
