package org.stockwellness.adapter.out.persistence.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long>, PortfolioCustomRepository {
    List<Portfolio> findAllByMemberId(Long memberId);

    Optional<Portfolio> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndName(Long memberId, String name);
}
