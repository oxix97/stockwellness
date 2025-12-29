package org.stockwellness.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioJpaRepository;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito 확장 사용
@DisplayName("Portfolio Service 테스트")
class PortfolioServiceTest {

    @InjectMocks
    PortfolioService portfolioService;

    @Mock
    PortfolioJpaRepository portfolioRepository;

    // 테스트 데이터
    final Long memberId = 1L;
    PortfolioCreateRequest createRequest;
    PortfolioUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // [Given] 공통 요청 데이터 생성 (총 8조각)
        List<PortfolioItemRequest> items = List.of(
                new PortfolioItemRequest("AAPL", 4, AssetType.STOCK),
                new PortfolioItemRequest("KRW", 4, AssetType.CASH)
        );

        createRequest = new PortfolioCreateRequest("내 연금", "설명", items);
        updateRequest = new PortfolioUpdateRequest(items);
    }

    @Nested
    @DisplayName("포트폴리오 생성 (Create)")
    class Create {

        @Test
        @DisplayName("성공: 중복된 이름이 없고 데이터가 유효하면 포트폴리오가 생성된다")
        void success() {
            // given
            given(portfolioRepository.existsByMemberIdAndName(memberId, createRequest.name()))
                    .willReturn(false); // 중복 아님

            // save 호출 시, 입력된 엔티티를 그대로 리턴한다고 가정 (ID만 임의 부여)
            given(portfolioRepository.save(any(Portfolio.class)))
                    .willAnswer(invocation -> {
                        Portfolio p = invocation.getArgument(0);
                        // Reflection 등을 쓰지 않고 Mocking 관점에서는 객체가 반환됨을 확인
                        return p;
                    });

            // when
            Long resultId = portfolioService.createPortfolio(memberId, createRequest);

            // then
            // 1. 중복 검사가 호출되었는지 확인
            verify(portfolioRepository).existsByMemberIdAndName(memberId, createRequest.name());
            // 2. 저장이 호출되었는지 확인
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 포트폴리오 이름이면 예외가 발생한다")
        void fail_duplicate_name() {
            // given
            given(portfolioRepository.existsByMemberIdAndName(memberId, createRequest.name()))
                    .willReturn(true); // 이미 존재함

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(memberId, createRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PORTFOLIO_NAME); // ErrorCode 추가 필요

            // save는 호출되지 않아야 함
            verify(portfolioRepository, times(0)).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("포트폴리오 조회 (Read)")
    class Read {

        @Test
        @DisplayName("성공: 내 포트폴리오를 상세 조회하면 정보를 반환한다")
        void success() {
            // given
            Long portfolioId = 100L;
            Portfolio mockPortfolio = Portfolio.create(memberId, "내 연금", "설명");
            // (필요 시 Reflection으로 ID 주입 가능하지만, 여기선 흐름만 검증)

            given(portfolioRepository.findWithItems(portfolioId, memberId))
                    .willReturn(Optional.of(mockPortfolio));

            // when
            PortfolioResponse response = portfolioService.getPortfolio(memberId, portfolioId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("내 연금");
        }

        @Test
        @DisplayName("실패: 존재하지 않거나 내 것이 아닌 포트폴리오는 예외가 발생한다")
        void fail_not_found() {
            // given
            Long portfolioId = 999L;
            given(portfolioRepository.findWithItems(portfolioId, memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getPortfolio(memberId, portfolioId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PORTFOLIO_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("포트폴리오 수정 (Update)")
    class Update {

        @Test
        @DisplayName("성공: 포트폴리오 구성 종목을 변경한다")
        void success() {
            // given
            Long portfolioId = 100L;
            Portfolio mockPortfolio = Portfolio.create(memberId, "기존 포트", "설명");

            given(portfolioRepository.findByIdAndMemberId(portfolioId, memberId))
                    .willReturn(Optional.of(mockPortfolio));

            // when
            portfolioService.updatePortfolio(memberId, portfolioId, updateRequest);

            // then
            // 포트폴리오 내부 아이템이 업데이트 되었는지 확인 (도메인 로직은 Unit Test에서 검증했으므로 호출 여부만 확인)
            assertThat(mockPortfolio.getTotalPieces()).isEqualTo(8);
        }

        @Test
        @DisplayName("실패: 수정하려는 포트폴리오가 없으면 예외가 발생한다")
        void fail_not_found() {
            // given
            Long portfolioId = 999L;
            given(portfolioRepository.findByIdAndMemberId(portfolioId, memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.updatePortfolio(memberId, portfolioId, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PORTFOLIO_NOT_FOUND);
        }
    }
}
