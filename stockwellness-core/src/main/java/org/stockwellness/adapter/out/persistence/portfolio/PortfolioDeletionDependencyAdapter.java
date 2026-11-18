package org.stockwellness.adapter.out.persistence.portfolio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.DeletePortfolioDependenciesPort;
import org.stockwellness.domain.portfolio.Portfolio;

@Component
public class PortfolioDeletionDependencyAdapter implements DeletePortfolioDependenciesPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void deleteByPortfolioId(Long portfolioId) {
        // Serialize dependent-row cleanup with async stats inserts guarded by the portfolio FK.
        entityManager.find(Portfolio.class, portfolioId, LockModeType.PESSIMISTIC_WRITE);
        entityManager.createQuery("delete from PortfolioStats stats where stats.id = :portfolioId")
                .setParameter("portfolioId", portfolioId)
                .executeUpdate();
        entityManager.createQuery("delete from AdvisorReport report where report.portfolioId = :portfolioId")
                .setParameter("portfolioId", portfolioId)
                .executeUpdate();
    }
}
