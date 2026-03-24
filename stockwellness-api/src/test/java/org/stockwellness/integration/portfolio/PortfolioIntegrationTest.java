package org.stockwellness.integration.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setUp() {
        Member member = Member.register("test@example.com", "tester", LoginType.GOOGLE);
        ReflectionTestUtils.setField(member, "role", MemberRole.USER);
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        ReflectionTestUtils.setField(member, "notificationRebalancing", true);
        ReflectionTestUtils.setField(member, "notificationMarketAlert", false);
        ReflectionTestUtils.setField(member, "notificationNewListing", false);
        ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
        memberRepository.save(member);

        // 테스트용 종목 생성
        Stock samsung = StockFixture.createSamsung();
        ReflectionTestUtils.setField(samsung, "createdAt", LocalDateTime.now());
        stockRepository.save(samsung);
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("포트폴리오 생성 통합 테스트: 생성 후 목록 조회 시 포함되어야 한다")
    void createAndGetPortfolio_Success() throws Exception {
        // given: 포트폴리오 생성 요청
        PortfolioItemRequest itemRequest = new PortfolioItemRequest("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        PortfolioCreateRequest createRequest = new PortfolioCreateRequest("내 통합 포트폴리오", "설명", List.of(itemRequest));

        // when: 생성 요청
        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S001"));

        // then: 목록 조회 확인
        mockMvc.perform(get("/api/v1/portfolios"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("내 통합 포트폴리오"));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("백테스트 통합 테스트: 생성된 포트폴리오에 대해 백테스트 시뮬레이션을 실행한다")
    void runBacktest_Success() throws Exception {
        // [Given] 1. 포트폴리오 생성
        PortfolioItemRequest itemRequest = new PortfolioItemRequest("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        PortfolioCreateRequest createRequest = new PortfolioCreateRequest("백테스트 포트폴리오", "설명", List.of(itemRequest));

        String createResponse = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long portfolioId = objectMapper.readTree(createResponse).get("data").asLong();

        // [Given] 2. 백테스트 요청 데이터
        org.stockwellness.adapter.in.web.portfolio.dto.BacktestRequest backtestRequest = new org.stockwellness.adapter.in.web.portfolio.dto.BacktestRequest(
                "LUMP_SUM", BigDecimal.valueOf(10000000), "^KS11", "MONTHLY", null
        );

        // [When] 3. 백테스트 실행
        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/analysis/backtest", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backtestRequest)))
                .andDo(print())
                // [Then] 4. 결과 검증
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cagr").exists())
                .andExpect(jsonPath("$.data.totalReturnRate").exists())
                .andExpect(jsonPath("$.data.aiComment").exists());
    }
}
