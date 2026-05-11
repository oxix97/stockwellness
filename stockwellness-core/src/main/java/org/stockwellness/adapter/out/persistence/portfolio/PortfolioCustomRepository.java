package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.List;
import java.util.Optional;

import org.stockwellness.domain.portfolio.Portfolio;

public interface PortfolioCustomRepository {
    Optional<Portfolio> findWithItems(Long id, Long memberId);

    List<Portfolio> findAllByMemberIdWithItems(Long memberId);
}
