package org.stockwellness.integration.watchlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.adapter.out.persistence.watchlist.WatchlistGroupRepository;
import org.stockwellness.adapter.in.web.watchlist.dto.CreateWatchlistGroupRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.AddWatchlistItemRequest;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.integration.common.BaseIntegrationTest;
import org.stockwellness.support.annotation.MockMember;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Watchlist API E2E 통합 테스트")
class WatchlistIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistGroupRepository groupRepository;

    private Member savedMember;
    private Stock savedStock;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        memberRepository.deleteAll();
        stockRepository.deleteAll();
        savedStock = stockRepository.save(StockFixture.createSamsung());
        accessToken = loginAndGetToken("test@example.com", "tester");
    }

    @Test
    @DisplayName("관심그룹 생성 및 종목 추가 흐름이 정상 동작한다")
    void watchlist_flow_success() throws Exception {
        // 1. 관심그룹 생성
        CreateWatchlistGroupRequest createRequest = new CreateWatchlistGroupRequest("내 관심그룹");
        String createResponse = mockMvc.perform(post("/api/v1/watchlist/groups")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Long groupId = objectMapper.readTree(createResponse).get("data").asLong();

        // 2. 종목 추가
        AddWatchlistItemRequest addRequest = new AddWatchlistItemRequest(savedStock.getTicker(), "내 삼성전자 메모");
        mockMvc.perform(post("/api/v1/watchlist/groups/{groupId}/items", groupId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated());

        // 3. 관심그룹 목록 조회
        mockMvc.perform(get("/api/v1/watchlist/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("내 관심그룹"));
    }
}
