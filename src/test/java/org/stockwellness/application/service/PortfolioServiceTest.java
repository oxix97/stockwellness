package org.stockwellness.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.DeletePortfolioPort;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioPort;
import org.stockwellness.application.port.out.portfolio.SavePortfolioPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.exception.DuplicatePortfolioNameException;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.exception.InvalidStockCodeException;
import org.stockwellness.fixture.PortfolioFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio 서비스 단위 테스트")
class PortfolioServiceTest {

    @InjectMocks
    private PortfolioService portfolioService;

    @Mock
    private LoadPortfolioPort loadPortfolioPort;

    @Mock
    private SavePortfolioPort savePortfolioPort;

    @Mock
    private DeletePortfolioPort deletePortfolioPort;

    @Mock
    private LoadStockPort loadStockPort;

    @Nested
    @DisplayName("포트폴리오 생성 (Create)")
    class Create {

        @Test
        @DisplayName("성공: 데이터가 유효하면 포트폴리오를 저장하고 ID를 반환한다")
        void create_success() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of(
                    PortfolioFixture.createStockItem("AAPL", 4),
                    PortfolioFixture.createCashItem(4)
            ));

            given(loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(false);
            given(loadStockPort.existsByIsinCode("AAPL")).willReturn(true);
            given(savePortfolioPort.savePortfolio(any(Portfolio.class)))
                    .willReturn(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID));

            // when
            Long portfolioId = portfolioService.createPortfolio(command);

            // then
            assertThat(portfolioId).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
            verify(savePortfolioPort).savePortfolio(any(Portfolio.class));
        }

        @Test
        @DisplayName("실패: 동일한 이름의 포트폴리오가 이미 존재하면 예외가 발생한다")
        void fail_duplicate_name() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of());
            given(loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(command))
                    .isInstanceOf(DuplicatePortfolioNameException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 주식 종목 코드(ISIN)를 포함하면 예외가 발생한다")
        void fail_invalid_stock_code() {
            // given
            CreatePortfolioCommand command = PortfolioFixture.createCreateCommand(List.of(
                    PortfolioFixture.createStockItem("INVALID_CODE", 1)
            ));
            given(loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())).willReturn(false);
            given(loadStockPort.existsByIsinCode("INVALID_CODE")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(command))
                    .isInstanceOf(InvalidStockCodeException.class);
        }
    }

    @Nested
    @DisplayName("포트폴리오 조회 (Read)")
    class Read {

        @Test
        @DisplayName("성공: 내 포트폴리오 정보를 상세 조회한다")
        void read_success() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID);
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));

            // when
            PortfolioResponse response = portfolioService.getPortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID);

            // then
            assertThat(response.id()).isEqualTo(PortfolioFixture.PORTFOLIO_ID);
            assertThat(response.name()).isEqualTo(PortfolioFixture.NAME);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 포트폴리오를 조회하려고 하면 UNAUTHORIZED 예외가 발생한다")
        void fail_unauthorized() {
            // given
            Long otherMemberId = 999L;
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, otherMemberId))
                    .willReturn(Optional.empty());
            given(loadPortfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));

            // when & then
            assertThatThrownBy(() -> portfolioService.getPortfolio(otherMemberId, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioAccessDeniedException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 포트폴리오를 조회하면 PORTFOLIO_NOT_FOUND 예외가 발생한다")
        void fail_not_found() {
            // given
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.empty());
            given(loadPortfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.getPortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioNotFoundException.class);
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
                    PortfolioFixture.PORTFOLIO_ID, "수정된 이름", List.of(PortfolioFixture.updateStockItem("TSLA", 8))
            );

            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));
            given(loadPortfolioPort.existsPortfolioName(PortfolioFixture.MEMBER_ID, "수정된 이름")).willReturn(false);
            given(loadStockPort.existsByIsinCode("TSLA")).willReturn(true);

            // when
            portfolioService.updatePortfolio(command);

            // then
            assertThat(portfolio.getName()).isEqualTo("수정된 이름");
            assertThat(portfolio.getTotalPieces()).isEqualTo(8);
        }

        @Test
        @DisplayName("실패: 변경하려는 이름이 이미 다른 포트폴리오에서 사용 중이면 예외가 발생한다")
        void fail_duplicate_name_on_update() {
            // given
            Portfolio portfolio = PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID);
            UpdatePortfolioCommand command = PortfolioFixture.createUpdateCommand(
                    PortfolioFixture.PORTFOLIO_ID, "중복된 이름", List.of()
            );

            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(portfolio));
            given(loadPortfolioPort.existsPortfolioName(PortfolioFixture.MEMBER_ID, "중복된 이름")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioService.updatePortfolio(command))
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
            // 소유권 확인 로직이 loadPortfolioPort.loadPortfolio(id, memberId)를 사용할 것으로 예상
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));

            // when
            portfolioService.deletePortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID);

            // then
            verify(deletePortfolioPort).deletePortfolio(PortfolioFixture.PORTFOLIO_ID);
        }

        @Test
        @DisplayName("실패: 다른 사람의 포트폴리오를 삭제하려고 하면 UNAUTHORIZED 예외가 발생한다")
        void fail_unauthorized() {
            // given
            Long otherMemberId = 999L;
            // ID로는 존재함
            given(loadPortfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.of(PortfolioFixture.createEntity(PortfolioFixture.PORTFOLIO_ID)));
            // MemberId로는 존재하지 않음 (내 것이 아님)
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, otherMemberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.deletePortfolio(otherMemberId, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioAccessDeniedException.class);
            
            verify(deletePortfolioPort, times(0)).deletePortfolio(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 포트폴리오를 삭제하려고 하면 PORTFOLIO_NOT_FOUND 예외가 발생한다")
        void fail_not_found() {
            // given
            given(loadPortfolioPort.findById(PortfolioFixture.PORTFOLIO_ID))
                    .willReturn(Optional.empty());
            given(loadPortfolioPort.loadPortfolio(PortfolioFixture.PORTFOLIO_ID, PortfolioFixture.MEMBER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> portfolioService.deletePortfolio(PortfolioFixture.MEMBER_ID, PortfolioFixture.PORTFOLIO_ID))
                    .isInstanceOf(PortfolioNotFoundException.class);

            verify(deletePortfolioPort, times(0)).deletePortfolio(any());
        }
    }
}
