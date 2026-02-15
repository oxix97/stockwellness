package org.stockwellness.adapter.out.persistence.portfolio;

import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioCustomRepository {
    Optional<Portfolio> findWithItems(Long id, Long memberId);

    List<Portfolio> findAllByMemberIdWithItems(Long memberId);
}
