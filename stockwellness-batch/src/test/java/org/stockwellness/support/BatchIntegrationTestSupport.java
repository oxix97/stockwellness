package org.stockwellness.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.outbox.OutboxEventRepository;
import org.stockwellness.adapter.out.persistence.portfolio.AdvisorReportRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioItemRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorInsightRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.adapter.out.persistence.watchlist.WatchlistGroupRepository;
import org.stockwellness.adapter.out.persistence.watchlist.WatchlistItemRepository;
import org.stockwellness.application.port.out.notification.NotificationPort;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BatchIntegrationTestSupport extends InfrastructureTestSupport {

    @Autowired
    private AdvisorReportRepository advisorReportRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private PortfolioStatsRepository portfolioStatsRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private BenchmarkPriceRepository benchmarkPriceRepository;

    @Autowired
    private SectorInsightRepository sectorInsightRepository;

    @Autowired
    private MarketIndexRepository marketIndexRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    protected NotificationPort notificationPort;

    @MockitoBean
    protected RestTemplate restTemplate;

    @MockitoBean
    protected KisMasterClient kisMasterClient;

    @MockitoBean
    protected org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory redisConnectionFactory;

    @BeforeEach
    void cleanDatabase() {
        advisorReportRepository.deleteAllInBatch();
        portfolioItemRepository.deleteAllInBatch();
        portfolioStatsRepository.deleteAllInBatch();
        portfolioRepository.deleteAllInBatch();
        watchlistItemRepository.deleteAllInBatch();
        watchlistGroupRepository.deleteAllInBatch();
        stockPriceRepository.deleteAllInBatch();
        benchmarkPriceRepository.deleteAllInBatch();
        sectorInsightRepository.deleteAllInBatch();
        marketIndexRepository.deleteAllInBatch();
        outboxEventRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }
}
