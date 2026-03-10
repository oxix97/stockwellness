package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;

public interface PortfolioDiversificationUseCase {
    PortfolioDiversificationResult getDiversification(Long memberId, Long portfolioId);
}
