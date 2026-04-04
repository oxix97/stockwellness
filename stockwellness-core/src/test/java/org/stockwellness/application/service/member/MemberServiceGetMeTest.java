package org.stockwellness.application.service.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.fixture.MemberFixture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService.getMember 재현 테스트")
class MemberServiceGetMeTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private LoadMemberPort loadMemberPort;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private StockPricePort stockPricePort;

    @Test
    @DisplayName("현금 자산이 포함된 포트폴리오를 가진 회원의 정보를 조회한다")
    void getMember_with_cash_item() {
        // given
        Long memberId = 1L;
        Member member = MemberFixture.createMember();
        given(loadMemberPort.loadMember(memberId)).willReturn(Optional.of(member));

        Portfolio portfolio = Portfolio.create(memberId, "Test Portfolio", "Description");
        PortfolioItem cashItem = PortfolioItem.createCash(BigDecimal.valueOf(1000000), "KRW");
        portfolio.updateItems(List.of(cashItem));

        given(portfolioPort.loadAllPortfolios(memberId)).willReturn(List.of(portfolio));
        
        // 일괄 조회 Mock 설정
        given(stockPricePort.findAllLatestByTickers(List.of("CASH"))).willReturn(Map.of());

        // when
        MemberResult result = memberService.getMember(memberId);

        // then
        assertThat(result).isNotNull();
        // 수정 후: 현금 가치가 1,000,000으로 정상 계산됨
        // 총 매수 금액: 1,000,000
        // 총 평가 금액: 1,000,000 (CASH 는 1.0으로 계산됨)
        // 수익률: 0.0%
        assertThat(result.portfolioSummary().totalAssetAmount()).isEqualTo(1000000L);
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("포트폴리오가 없는 회원의 정보를 조회한다")
    void getMember_with_no_portfolios() {
        // given
        Long memberId = 1L;
        Member member = MemberFixture.createMember();
        given(loadMemberPort.loadMember(memberId)).willReturn(Optional.of(member));
        given(portfolioPort.loadAllPortfolios(memberId)).willReturn(List.of());

        // when
        MemberResult result = memberService.getMember(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.portfolioSummary().totalAssetAmount()).isEqualTo(0L);
        assertThat(result.portfolioSummary().totalReturnRate()).isEqualTo(0.0);
    }
}
