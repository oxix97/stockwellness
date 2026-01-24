package org.stockwellness.adapter.in.web.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.global.error.ErrorCode;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트 종료 후 자동 롤백
@DisplayName("Portfolio 통합 테스트 (End-to-End)")
class PortfolioIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PortfolioRepository portfolioRepository;

    // 테스트용 사용자 ID (String "1" -> ArgumentResolver가 Long 1L로 변환)
    static final String MY_ID = "1";
    static final String OTHER_ID = "99";

    @BeforeEach
    void setUp() {
        // [Given] 초기 데이터 세팅: 사용자 1의 기본 포트폴리오
        // DB를 비우고 시작하거나 @Transactional에 의존 (여기서는 안전하게 추가만 함)
    }

    @Nested
    @DisplayName("성공 시나리오 (Success)")
    class Success {

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("생성 및 조회: 정상적인 포트폴리오를 생성하고 상세 조회한다")
        void create_and_get_portfolio() throws Exception {
            // given
            List<PortfolioItemRequest> items = List.of(
                    new PortfolioItemRequest("AAPL", 4, AssetType.STOCK),
                    new PortfolioItemRequest("KRW", 4, AssetType.CASH)
            );
            PortfolioCreateRequest request = new PortfolioCreateRequest("성공 포트폴리오", "테스트", items);

            // when (Create)
            mockMvc.perform(post("/api/v1/portfolios")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));

            // 데이터 확인을 위해 DB에서 ID 조회
            Portfolio saved = portfolioRepository.findAll().stream()
                    .filter(p -> p.getName().equals("성공 포트폴리오"))
                    .findFirst().orElseThrow();

            // when (Get Detail)
            mockMvc.perform(get("/api/v1/portfolios/" + saved.getId())
                            .contentType(APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("성공 포트폴리오")))
                    .andExpect(jsonPath("$.totalPieces", is(8)))
                    .andExpect(jsonPath("$.items", hasSize(2)));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("수정: 포트폴리오 구성을 8조각 조건에 맞춰 수정한다")
        void update_portfolio() throws Exception {
            // given
            Portfolio portfolio = savePortfolio(1L, "수정전", "설명");
            List<PortfolioItemRequest> newItems = List.of(
                    new PortfolioItemRequest("TSLA", 8, AssetType.STOCK)
            );
            PortfolioUpdateRequest updateRequest = new PortfolioUpdateRequest(newItems);

            // when
            mockMvc.perform(put("/api/v1/portfolios/" + portfolio.getId())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isNoContent()); // 204

            // then (DB Verify)
            Portfolio updated = portfolioRepository.findById(portfolio.getId()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(updated.getItems()).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(updated.getItems().get(0).getIsinCode()).isEqualTo("TSLA");
        }
    }

    @Nested
    @DisplayName("실패 시나리오 (Failure) - 검증 및 보안")
    class Failure {

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("검증: 총 조각 수가 8개를 초과하면 400 Bad Request")
        void fail_too_many_pieces() throws Exception {
            // given (5 + 4 = 9조각)
            List<PortfolioItemRequest> items = List.of(
                    new PortfolioItemRequest("AAPL", 5, AssetType.STOCK),
                    new PortfolioItemRequest("MSFT", 4, AssetType.STOCK)
            );
            PortfolioCreateRequest request = new PortfolioCreateRequest("과욕 포트폴리오", "", items);

            // when & then
            mockMvc.perform(post("/api/v1/portfolios")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    // ProblemDetail 혹은 ErrorResponse 구조 확인
                    .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_PORTFOLIO_TOTAL_PIECE.name()));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("검증: 총 조각 수가 1개 미만(0개)이면 400 Bad Request")
        void fail_zero_total_pieces() throws Exception {
            // given (빈 리스트)
            List<PortfolioItemRequest> items = List.of();
            PortfolioCreateRequest request = new PortfolioCreateRequest("공기 포트폴리오", "", items);

            // when & then
            mockMvc.perform(post("/api/v1/portfolios")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_PORTFOLIO_TOTAL_PIECE.name()));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("검증: 개별 아이템 조각 수가 1 미만이면 400 Bad Request")
        void fail_invalid_item_piece() throws Exception {
            // given (0조각 아이템 포함)
            List<PortfolioItemRequest> items = List.of(
                    new PortfolioItemRequest("AAPL", 0, AssetType.STOCK),
                    new PortfolioItemRequest("KRW", 8, AssetType.CASH)
            );
            PortfolioCreateRequest request = new PortfolioCreateRequest("잘못된 아이템", "", items);

            // when & then
            mockMvc.perform(post("/api/v1/portfolios")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_ITEM_PIECE_COUNT.name()));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("중복: 이미 존재하는 이름으로 생성 시도 시 409 Conflict")
        void fail_duplicate_name() throws Exception {
            // given
            savePortfolio(1L, "중복이름", "설명");

            List<PortfolioItemRequest> items = List.of(new PortfolioItemRequest("KRW", 8, AssetType.CASH));
            PortfolioCreateRequest request = new PortfolioCreateRequest("중복이름", "설명2", items);

            // when & then
            mockMvc.perform(post("/api/v1/portfolios")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value(ErrorCode.DUPLICATE_PORTFOLIO_NAME.name()));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("보안: 다른 사용자의 포트폴리오를 수정하려 하면 404 Not Found (혹은 403)")
        void fail_update_others_portfolio() throws Exception {
            // given: 사용자 99(OTHER)가 만든 포트폴리오
            Portfolio otherPortfolio = savePortfolio(Long.parseLong(OTHER_ID), "남의꺼", "");

            // when: 사용자 1(MY)이 수정 시도
            List<PortfolioItemRequest> items = List.of(new PortfolioItemRequest("KRW", 8, AssetType.CASH));
            PortfolioUpdateRequest request = new PortfolioUpdateRequest(items);

            mockMvc.perform(put("/api/v1/portfolios/" + otherPortfolio.getId())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // Service 로직상 내 것이 아니면 조회 안됨 -> 404
                    .andExpect(jsonPath("$.title").value(ErrorCode.PORTFOLIO_NOT_FOUND.name()));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("조회: 존재하지 않는 포트폴리오 ID 요청 시 404 Not Found")
        void fail_not_found_id() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/portfolios/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value(ErrorCode.PORTFOLIO_NOT_FOUND.name()));
        }

        @Test
        @DisplayName("인증: 토큰 없이(로그인 안함) 접근 시 401 Unauthorized")
            // @WithMockUser 없음 -> Anonymous User
        void fail_unauthorized() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/portfolios"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized()); // Spring Security 설정에 따라 403일 수도 있음
        }
    }

    // --- Helper Methods ---
    Portfolio savePortfolio(Long memberId, String name, String desc) {
        Portfolio p = Portfolio.create(memberId, name, desc);
        p.updateItems(List.of(PortfolioItem.createCash(8)));
        return portfolioRepository.save(p);
    }
}