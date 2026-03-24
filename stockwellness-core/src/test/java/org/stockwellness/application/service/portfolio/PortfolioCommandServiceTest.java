package org.stockwellness.application.service.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.exception.DuplicatePortfolioNameException;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.exception.InvalidStockCodeException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioCommandServiceTest {

    @InjectMocks
    private PortfolioCommandService portfolioCommandService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPort stockPort;

    private static final Long MEMBER_ID = 1L;
    private static final Long PORTFOLIO_ID = 100L;

    @Test
    @DisplayName("포트폴리오 생성: 중복되지 않은 이름과 유효한 종목으로 생성에 성공한다")
    void createPortfolio_Success() {
        // given
        CreatePortfolioCommand.PortfolioItemCommand itemCommand = 
                new CreatePortfolioCommand.PortfolioItemCommand("005930", BigDecimal.TEN, BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        CreatePortfolioCommand command = new CreatePortfolioCommand(MEMBER_ID, "내 포트폴리오", "설명", List.of(itemCommand));

        given(portfolioPort.existsPortfolioName(MEMBER_ID, "내 포트폴리오")).willReturn(false);
        given(stockPort.existsByTicker("005930")).willReturn(true);
        given(portfolioPort.savePortfolio(any(Portfolio.class))).willAnswer(invocation -> {
            Portfolio portfolio = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(portfolio, "id", PORTFOLIO_ID);
            return portfolio;
        });

        // when
        Long savedId = portfolioCommandService.createPortfolio(command);

        // then
        assertThat(savedId).isEqualTo(PORTFOLIO_ID);
        verify(portfolioPort).savePortfolio(any(Portfolio.class));
    }

    @Test
    @DisplayName("포트폴리오 생성 실패: 이미 존재하는 포트폴리오 이름인 경우 예외가 발생한다")
    void createPortfolio_DuplicateName() {
        // given
        CreatePortfolioCommand command = new CreatePortfolioCommand(MEMBER_ID, "중복이름", "설명", List.of());
        given(portfolioPort.existsPortfolioName(MEMBER_ID, "중복이름")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> portfolioCommandService.createPortfolio(command))
                .isInstanceOf(DuplicatePortfolioNameException.class);
    }

    @Test
    @DisplayName("포트폴리오 생성 실패: 존재하지 않는 종목 코드가 포함된 경우 예외가 발생한다")
    void createPortfolio_InvalidStock() {
        // given
        CreatePortfolioCommand.PortfolioItemCommand itemCommand = 
                new CreatePortfolioCommand.PortfolioItemCommand("INVALID", BigDecimal.TEN, BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
        CreatePortfolioCommand command = new CreatePortfolioCommand(MEMBER_ID, "포트", "설명", List.of(itemCommand));

        given(portfolioPort.existsPortfolioName(MEMBER_ID, "포트")).willReturn(false);
        given(stockPort.existsByTicker("INVALID")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> portfolioCommandService.createPortfolio(command))
                .isInstanceOf(InvalidStockCodeException.class);
    }

    @Test
    @DisplayName("포트폴리오 수정: 이름과 종목 구성을 정상적으로 업데이트한다")
    void updatePortfolio_Success() {
        // given
        Portfolio existingPortfolio = Portfolio.create(MEMBER_ID, "기존이름", "기존설명");
        UpdatePortfolioCommand command = new UpdatePortfolioCommand(MEMBER_ID, PORTFOLIO_ID, "새이름", "새설명", List.of());

        given(portfolioPort.loadPortfolio(PORTFOLIO_ID, MEMBER_ID)).willReturn(Optional.of(existingPortfolio));
        given(portfolioPort.existsPortfolioName(MEMBER_ID, "새이름")).willReturn(false);

        // when
        portfolioCommandService.updatePortfolio(command);

        // then
        assertThat(existingPortfolio.getName()).isEqualTo("새이름");
        assertThat(existingPortfolio.getDescription()).isEqualTo("새설명");
    }

    @Test
    @DisplayName("포트폴리오 삭제: 소유권을 확인하고 삭제를 호출한다")
    void deletePortfolio_Success() {
        // given
        Portfolio portfolio = Portfolio.create(MEMBER_ID, "삭제할 포트", "설명");
        given(portfolioPort.loadPortfolio(PORTFOLIO_ID, MEMBER_ID)).willReturn(Optional.of(portfolio));

        // when
        portfolioCommandService.deletePortfolio(MEMBER_ID, PORTFOLIO_ID);

        // then
        verify(portfolioPort).deletePortfolio(PORTFOLIO_ID);
    }

    @Test
    @DisplayName("소유권 확인 실패: 포트폴리오는 존재하지만 소유자가 다른 경우 예외가 발생한다")
    void loadOwnedPortfolio_AccessDenied() {
        // given
        given(portfolioPort.loadPortfolio(PORTFOLIO_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(portfolioPort.findById(PORTFOLIO_ID)).willReturn(Optional.of(mock(Portfolio.class)));

        // when & then
        assertThatThrownBy(() -> portfolioCommandService.deletePortfolio(MEMBER_ID, PORTFOLIO_ID))
                .isInstanceOf(PortfolioAccessDeniedException.class);
    }

    @Test
    @DisplayName("소유권 확인 실패: 포트폴리오 자체가 존재하지 않는 경우 예외가 발생한다")
    void loadOwnedPortfolio_NotFound() {
        // given
        given(portfolioPort.loadPortfolio(PORTFOLIO_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(portfolioPort.findById(PORTFOLIO_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> portfolioCommandService.deletePortfolio(MEMBER_ID, PORTFOLIO_ID))
                .isInstanceOf(PortfolioNotFoundException.class);
    }
}
