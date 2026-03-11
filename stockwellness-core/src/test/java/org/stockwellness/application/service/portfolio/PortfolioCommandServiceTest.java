package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.exception.DuplicatePortfolioNameException;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.stock.exception.InvalidStockCodeException;
import org.stockwellness.fixture.PortfolioFixture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioCommandService 단위 테스트")
class PortfolioCommandServiceTest {

    @InjectMocks
    private PortfolioCommandService portfolioCommandService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPort stockPort;

    @Nested
    @DisplayName("포트폴리오 생성 (Create)")
    class Create {

        @Test
        @DisplayName("성공: 데이터가 유효하면 포트폴리오를 저장하고 ID를 반환한다")
        void create_success() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of(
                    PortfolioFixture.createStockItem("AAPL", BigDecimal.TEN, BigDecimal.valueOf(150)),
                    PortfolioFixture.createCashItem(BigDecimal.valueOf(500))
            ));

            given(portfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(false);
            given(stockPort.existsByTicker("AAPL")).willReturn(true);
            given(portfolioPort.savePortfolio(any(Portfolio.class)))
                    .willReturn(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID));

            // when
            Long portfolioId = portfolioCommandService.createPortfolio(command);

            // then
            assertThat(portfolioId).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
            verify(portfolioPort).savePortfolio(any(Portfolio.class));
        }

        @Test
        @DisplayName("실패: 동일한 이름의 포트폴리오가 이미 존재하면 예외가 발생한다")
        void fail_duplicate_name() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of());
            given(portfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioCommandService.createPortfolio(command))
                    .isInstanceOf(DuplicatePortfolioNameException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주식 종목 코드(ISIN)를 포함하면 예외가 발생한다")
        void fail_invalid_stock_code() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of(
                    PortfolioFixture.createStockItem("INVALID_CODE", BigDecimal.ONE, BigDecimal.valueOf(100))
            ));
            given(portfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(false);
            given(stockPort.existsByTicker("INVALID_CODE")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> portfolioCommandService.createPortfolio(command))
                    .isInstanceOf(InvalidStockCodeException.class);
        }
    }

    @Nested
    @DisplayName("포트폴리오 수정 (Update)")
    class Update {

        @Test
        @DisplayName("성공: 이름, 설명 및 종목 구성을 모두 수정한다")
        void update_success() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID);
            UpdatePortfolioCommand command = PortfolioFixture.createUpdateCommand(
                    PortfolioFixture.PORTFOLIO_ID, "수정된 이름", List.of(PortfolioFixture.updateStockItem("TSLA", BigDecimal.ONE, BigDecimal.valueOf(200)))
            );

            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));
            given(portfolioPort.existsPortfolioName(PortfolioFixture.MEMBER_ID, "수정된 이름")).willReturn(false);
            given(stockPort.existsByTicker("TSLA")).willReturn(true);

            // when
            portfolioCommandService.updatePortfolio(command);

            // then
            assertThat(portfolio.getName()).isEqualTo("수정된 이름");
            assertThat(portfolio.calculateTotalPurchaseAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        }

        @Test
        @DisplayName("실패: 변경하려는 이름이 이미 다른 포트폴리오에서 사용 중이면 예외가 발생한다")
        void fail_duplicate_name_on_update() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID);
            UpdatePortfolioCommand command = PortfolioFixture.createUpdateCommand(
                    PortfolioFixture.PORTFOLIO_ID, "중복된 이름", List.of()
            );

            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));
            given(portfolioPort.existsPortfolioName(PortfolioFixture.MEMBER_ID, "중복된 이름")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioCommandService.updatePortfolio(command))
                    .isInstanceOf(DuplicatePortfolioNameException.class);
        }
    }

    @Nested
    @DisplayName("포트폴리오 삭제 (Delete)")
    class Delete {

        @Test
        @DisplayName("성공: 내 포트폴리오를 삭제한다")
        void delete_success() {
            // given
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));

            // when
            portfolioCommandService.deletePortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID);

            // then
            verify(portfolioPort).deletePortfolio(PortfolioFixture.PORTFOLIO_ID);
        }

        @Test
        @DisplayName("실패: 다른 사람의 포트폴리오를 삭제하려고 하면 UNAUTHORIZED 예외 가 발생한다")
        void fail_unauthorized() {
            // given
            Long otherMemberId = 999L;
            given(portfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));
            given(portfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, otherMemberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioCommandService.deletePortfolio(otherMemberId, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioAccessDeniedException.class);

            verify(portfolioPort, times(0)).deletePortfolio(any());
        }
    }
}
