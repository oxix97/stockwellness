package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.BacktestEngine;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.global.util.JsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@Import({PortfolioStatBatchService.class, JsonUtil.class, org.stockwellness.config.QueryDslConfig.class})
class PortfolioStatBatchServiceTest {

    @Autowired
    private PortfolioStatBatchService portfolioStatBatchService;

    @Autowired
    private PortfolioStatsRepository portfolioStatsRepository;

    @MockBean
    private SimulationDataProvider simulationDataProvider;

    @MockBean
    private BacktestEngine backtestEngine;

    @MockBean
    private OutboxPort outboxPort;

    @MockBean
    private PortfolioAnalysisService portfolioAnalysisService;

    @MockBean
    private PortfolioPort portfolioPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("테스트 컨텍스트 로딩 확인")
    void contextLoads() {
        assertThat(portfolioStatBatchService).isNotNull();
    }
}
