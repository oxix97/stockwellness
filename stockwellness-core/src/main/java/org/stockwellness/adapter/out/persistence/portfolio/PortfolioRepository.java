package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.portfolio.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long>, PortfolioCustomRepository {
    List<Portfolio> findAllByMemberId(Long memberId);

    Optional<Portfolio> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndName(Long memberId, String name);

    @Query("SELECT DISTINCT p.id FROM Portfolio p JOIN p.items i WHERE i.symbol IN :symbols")
    List<Long> findPortfolioIdsBySymbols(@Param("symbols") List<String> symbols);

    @Query("SELECT p.id FROM Portfolio p")
    List<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Portfolio p LEFT JOIN FETCH p.items WHERE p.id IN :ids")
    List<Portfolio> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);
}
