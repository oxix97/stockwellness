package org.stockwellness.application.service.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.BacktestEngine;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.application.service.portfolio.internal.SimulationData;
import org.stockwellness.application.service.portfolio.internal.SimulationDataProvider;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.global.util.JsonUtil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({PortfolioStatBatchService.class, JsonUtil.class, QueryDslConfig.class, JpaConfig.class})
class PortfolioStatBatchServiceTest {

    @Autowired
    private PortfolioStatBatchService portfolioStatBatchService;

    @Autowired
    private PortfolioStatsRepository portfolioStatsRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private MemberRepository memberRepository;

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

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
    }

    @Test
    @DisplayName("테스트 컨텍스트 로딩 확인")
    void contextLoads() {
        assertThat(portfolioStatBatchService).isNotNull();
    }

    @Nested
    @DisplayName("포트폴리오 통계 배치 업데이트 테스트")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class UpdateStats {

        @Test
        @DisplayName("신규 저장: 통계가 없는 포트폴리오는 새로 생성된다")
        void createNew() {
            // given
            Long portfolioId = createTestPortfolio("test@test.com", "테스터1");
            Portfolio portfolio = loadPortfolioWithItems(portfolioId);

            given(portfolioPort.loadAllWithItems(anyList())).willReturn(List.of(portfolio));
            setupMocks();

            // when
            portfolioStatBatchService.updatePortfolioStatsBatch(List.of(portfolioId));

            // then
            assertStats(portfolioId, "15.5", "1.2", "0.9");
            verify(outboxPort, times(1)).save(any());
        }

        @Test
        @DisplayName("기존 업데이트: 이미 통계가 있는 포트폴리오는 값이 업데이트된다")
        void updateExisting() {
            // given
            Long portfolioId = createTestPortfolio("update@test.com", "테스터2");
            Portfolio portfolio = loadPortfolioWithItems(portfolioId);
            
            transactionTemplate.executeWithoutResult(status -> {
                portfolioStatsRepository.save(PortfolioStats.create(portfolio, LocalDate.now().minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            });

            given(portfolioPort.loadAllWithItems(anyList())).willReturn(List.of(portfolio));
            setupMocks();

            // when
            portfolioStatBatchService.updatePortfolioStatsBatch(List.of(portfolioId));

            // then
            assertStats(portfolioId, "15.5", "1.2", "0.9");
        }

        @Test
        @DisplayName("부분 실패: 일부 포트폴리오 작업 중 예외가 발생해도 다른 포트폴리오는 처리된다")
        void partialFailure() {
            // given
            Long id1 = createTestPortfolio("user1@test.com", "회원1");
            Long id2 = createTestPortfolio("user2@test.com", "회원2");
            Portfolio p1 = loadPortfolioWithItems(id1);
            Portfolio p2 = loadPortfolioWithItems(id2);

            given(portfolioPort.loadAllWithItems(anyList())).willReturn(List.of(p1, p2));
            
            // 첫 번째 포트폴리오 처리 시 예외 발생 유도
            given(backtestEngine.runLumpSum(any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .willThrow(new RuntimeException("엔진 오류")) // 첫 호출
                    .willReturn(new BacktestResult(List.of(), BigDecimal.ZERO, BigDecimal.valueOf(10.0), BigDecimal.ZERO, BigDecimal.valueOf(1.0), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1.0), BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), List.of(), null)); // 두 번째 호출

            given(simulationDataProvider.loadData(anyList(), any(), any(), any())).willReturn(new SimulationData(Map.of("005930", List.of()), Map.of()));
            given(portfolioAnalysisService.calculateBenchmarkReturn(anyString(), any(), any())).willReturn(BigDecimal.valueOf(5.0));

            // when
            portfolioStatBatchService.updatePortfolioStatsBatch(List.of(id1, id2));

            // then
            transactionTemplate.executeWithoutResult(status -> {
                assertThat(portfolioStatsRepository.findByPortfolioId(id1)).isEmpty();
                assertThat(portfolioStatsRepository.findByPortfolioId(id2)).isPresent();
            });
        }

        @Test
        @DisplayName("자산 부재 건너뜀: 주식 자산이 없는 포트폴리오는 처리를 건너뛴다")
        void skipNoAssets() {
            // given
            Long portfolioId = transactionTemplate.execute(status -> {
                Member m = Member.register("empty@test.com", "무일푼", LoginType.GOOGLE);
                memberRepository.save(m);
                Portfolio p = Portfolio.create(m.getId(), "빈 포트폴리오", "");
                portfolioRepository.save(p);
                return p.getId();
            });
            Portfolio portfolio = loadPortfolioWithItems(portfolioId);

            given(portfolioPort.loadAllWithItems(anyList())).willReturn(List.of(portfolio));

            // when
            portfolioStatBatchService.updatePortfolioStatsBatch(List.of(portfolioId));

            // then
            transactionTemplate.executeWithoutResult(status -> {
                assertThat(portfolioStatsRepository.findByPortfolioId(portfolioId)).isEmpty();
            });
        }

        private Long createTestPortfolio(String email, String nickname) {
            return transactionTemplate.execute(status -> {
                Member member = Member.register(email, nickname, LoginType.GOOGLE);
                memberRepository.save(member);

                Portfolio portfolio = Portfolio.create(member.getId(), "포트폴리오", "설명");
                PortfolioItem item = PortfolioItem.createStock("005930", BigDecimal.TEN, BigDecimal.valueOf(50000), "KRW", BigDecimal.valueOf(100), LocalDate.now());
                portfolio.updateItems(List.of(item));
                
                portfolioRepository.save(portfolio);
                return portfolio.getId();
            });
        }

        private Portfolio loadPortfolioWithItems(Long id) {
            return transactionTemplate.execute(status -> {
                Portfolio p = portfolioRepository.findById(id).orElseThrow();
                p.getItems().size();
                return p;
            });
        }

        private void setupMocks() {
            given(simulationDataProvider.loadData(anyList(), any(), any(), any())).willReturn(new SimulationData(Map.of("005930", List.of()), Map.of()));
            given(backtestEngine.runLumpSum(any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(new BacktestResult(List.of(), BigDecimal.ZERO, BigDecimal.valueOf(15.5), BigDecimal.ZERO, BigDecimal.valueOf(1.2), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(0.9), BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), List.of(), null));
            given(portfolioAnalysisService.calculateBenchmarkReturn(anyString(), any(), any())).willReturn(BigDecimal.valueOf(5.0));
        }

        private void assertStats(Long id, String mdd, String sharpe, String beta) {
            transactionTemplate.executeWithoutResult(status -> {
                var stats = portfolioStatsRepository.findByPortfolioId(id).orElseThrow();
                assertThat(stats.getMdd()).isEqualByComparingTo(mdd);
                assertThat(stats.getSharpeRatio()).isEqualByComparingTo(sharpe);
                assertThat(stats.getBeta()).isEqualByComparingTo(beta);
            });
        }
    }
}
