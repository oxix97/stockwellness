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
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.fixture.PortfolioFixture;

import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private org.stockwellness.application.port.out.stock.StockPricePort stockPricePort;

    @Mock
    private StockPort stockPort;

    @Nested
    @DisplayName("포트폴리오 조회 (Read)")
    class Read {

        @Test
        @DisplayName("성공: 내 포트폴리오 정보를 상세 조회한다")
        void read_success() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntityWithItems(
                    PortfolioFixture.PORTFOLIO_ID,
                    List.of(PortfolioItem.createStock("005930", BigDecimal.ONE, BigDecimal.valueOf(70000), "KRW"))
            );
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));
            given(stockPricePort.findAllLatestByTickers(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(java.util.Map.of());
            given(stockPort.loadStocksByTickers(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(List.of());

            // when
            PortfolioResponse response = portfolioQueryService.getPortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID);

            // then
            assertThat(response.id()).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
            assertThat(response.name()).isEqualTo(PortfolioFixture.NAME);
            assertThat(response.items()).isNotEmpty();
            assertThat(response.items())
                    .allSatisfy(item -> assertThat(item.name()).isNotBlank());
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

        @Test
        @DisplayName("성공: 내 모든 포트폴리오 목록을 조회한다")
        void read_all_success() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntityWithItems(
                    PortfolioFixture.PORTFOLIO_ID,
                    List.of(PortfolioItem.createStock("005930", BigDecimal.ONE, BigDecimal.valueOf(70000), "KRW"))
            );
            given(portfolioPort.loadAllPortfolios(PortfolioFixture.MEMBER_ID))
                    .willReturn(List.of(portfolio));
            given(stockPricePort.findAllLatestByTickers(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(java.util.Map.of());
            given(stockPort.loadStocksByTickers(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(List.of());

            // when
            List<PortfolioResponse> responses = portfolioQueryService.getMyPortfolios(PortfolioFixture.MEMBER_ID);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
        }
    }
}
