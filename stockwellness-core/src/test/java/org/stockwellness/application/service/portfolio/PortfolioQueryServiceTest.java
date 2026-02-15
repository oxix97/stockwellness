package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.fixture.PortfolioFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioQueryService 단위 테스트")
class PortfolioQueryServiceTest {

    @InjectMocks
    private PortfolioQueryService portfolioQueryService;

    @Mock
    private PortfolioPort portfolioPort;

    @Nested
    @DisplayName("포트폴리오 조회 (Read)")
    class Read {

        @Test
        @DisplayName("성공: 내 포트폴리오 정보를 상세 조회한다")
        void read_success() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID);
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));

            // when
            PortfolioResponse response = portfolioQueryService.getPortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID);

            // then
            assertThat(response.id()).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
            assertThat(response.name()).isEqualTo(PortfolioFixture.NAME);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 포트폴리오를 조회하려고 하면 UNAUTHORIZED 예외가 발생한다")
        void fail_unauthorized() {
            // given
            Long otherMemberId = 999L;
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, otherMemberId))
                    .willReturn(Optional.empty());
            given(portfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));

            // when & then
            assertThatThrownBy(() -> portfolioQueryService.getPortfolio(otherMemberId, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioAccessDeniedException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 포트폴리오를 조회하면 PORTFOLIO_NOT_FOUND 예외가 발생한다")
        void fail_not_found() {
            // given
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.empty());
            given(portfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioQueryService.getPortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioNotFoundException.class);
        }
    }
}