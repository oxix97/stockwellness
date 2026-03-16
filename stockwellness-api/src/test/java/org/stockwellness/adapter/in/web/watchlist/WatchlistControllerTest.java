package org.stockwellness.adapter.in.web.watchlist;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.watchlist.dto.*;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Watchlist 통합 테스트 (RestDocs)")
class WatchlistControllerTest extends RestDocsSupport {

    @MockitoBean
    private WatchlistUseCase watchlistUseCase;

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹을 생성한다")
    void createGroup() throws Exception {
        // given
        CreateWatchlistGroupRequest request = new CreateWatchlistGroupRequest("내 관심 그룹");
        given(watchlistUseCase.createGroup(1L, "내 관심 그룹")).willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/v1/watchlist/groups")
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("watchlist-group-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 그룹 생성")
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .requestFields(fieldWithPath("name").description("그룹 이름"))
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹 목록을 조회한다")
    void getGroups() throws Exception {
        // given
        List<WatchlistGroupResponse> response = List.of(
                new WatchlistGroupResponse(10L, "기본 그룹", 2L),
                new WatchlistGroupResponse(11L, "테크주", 5L)
        );
        given(watchlistUseCase.getGroups(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/watchlist/groups")
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("watchlist-group-list",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 그룹 목록 조회")
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .responseFields(
                                        fieldWithPath("[].id").description("그룹 ID"),
                                        fieldWithPath("[].name").description("그룹 이름"),
                                        fieldWithPath("[].itemCount").description("포함된 종목 수")
                                )
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹에 종목을 추가한다")
    void addItem() throws Exception {
        // given
        AddWatchlistItemRequest request = new AddWatchlistItemRequest("005930", "삼성전자 메모");

        // when & then
        mockMvc.perform(post("/api/v1/watchlist/groups/{groupId}/items", 10L)
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("watchlist-item-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 종목 추가")
                                .pathParameters(parameterWithName("groupId").description("그룹 ID"))
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .requestFields(
                                        fieldWithPath("ticker").description("종목 티커"),
                                        fieldWithPath("note").description("투자 메모 (선택)")
                                )
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹 내 종목의 메모를 수정한다")
    void updateItemNote() throws Exception {
        // given
        UpdateWatchlistItemNoteRequest request = new UpdateWatchlistItemNoteRequest("수정된 메모");

        // when & then
        mockMvc.perform(patch("/api/v1/watchlist/groups/{groupId}/items/{ticker}/note", 10L, "005930")
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andDo(document("watchlist-item-note-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 종목 메모 수정")
                                .pathParameters(
                                        parameterWithName("groupId").description("그룹 ID"),
                                        parameterWithName("ticker").description("종목 티커")
                                )
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .requestFields(fieldWithPath("note").description("수정할 메모 내용"))
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹에서 종목을 제거한다")
    void removeItem() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/watchlist/groups/{groupId}/items/{ticker}", 10L, "005930")
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .with(csrf())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(document("watchlist-item-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 종목 제거")
                                .pathParameters(
                                        parameterWithName("groupId").description("그룹 ID"),
                                        parameterWithName("ticker").description("종목 티커")
                                )
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("관심 그룹 내 종목 목록을 조회한다")
    void getItems() throws Exception {
        // given
        WatchlistItemListResponse response = new WatchlistItemListResponse("테크주", List.of(
                new WatchlistItemListResponse.WatchlistItemDetail("005930", "삼성전자", BigDecimal.valueOf(70000), BigDecimal.valueOf(-1.2), "메모 내용", BigDecimal.valueOf(45), "중립", "AI 요약 내용...")
        ));
        given(watchlistUseCase.getItems(1L, 10L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/watchlist/groups/{groupId}/items", 10L)
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("watchlist-item-list",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Watchlist")
                                .summary("관심 종목 목록 조회")
                                .pathParameters(parameterWithName("groupId").description("그룹 ID"))
                                .requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))
                                .responseFields(
                                        fieldWithPath("groupName").description("그룹 이름"),
                                        fieldWithPath("items[].ticker").description("종목 티커"),
                                        fieldWithPath("items[].name").description("종목명"),
                                        fieldWithPath("items[].currentPrice").description("현재가"),
                                        fieldWithPath("items[].fluctuationRate").description("등락률"),
                                        fieldWithPath("items[].note").description("투자 메모"),
                                        fieldWithPath("items[].rsi").description("RSI 지표"),
                                        fieldWithPath("items[].rsiStatus").description("RSI 상태"),
                                        fieldWithPath("items[].aiInsight").description("AI 한줄 분석")
                                )
                                .build())
                ));
    }
}
