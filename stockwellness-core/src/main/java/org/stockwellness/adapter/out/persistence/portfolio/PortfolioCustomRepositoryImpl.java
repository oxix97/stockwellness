package org.stockwellness.adapter.out.persistence.portfolio;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

import static org.stockwellness.domain.portfolio.QPortfolio.portfolio;
import static org.stockwellness.domain.portfolio.QPortfolioItem.portfolioItem;
import static org.stockwellness.domain.portfolio.advisor.QAdvisorReport.advisorReport;

@RequiredArgsConstructor
public class PortfolioCustomRepositoryImpl implements PortfolioCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Portfolio> findWithItems(Long id, Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(portfolio)
                        .leftJoin(portfolio.items, portfolioItem).fetchJoin()
                        .where(
                                portfolio.id.eq(id),
                                portfolio.memberId.eq(memberId)
                        )
                        .distinct()
                        .fetchOne()
        );
    }

    @Override
    public List<Portfolio> findAllByMemberIdWithItems(Long memberId) {
        return queryFactory
                .selectFrom(portfolio)
                .leftJoin(portfolio.items, portfolioItem).fetchJoin()
                .where(portfolio.memberId.eq(memberId))
                .distinct()
                .fetch();
    }
}
