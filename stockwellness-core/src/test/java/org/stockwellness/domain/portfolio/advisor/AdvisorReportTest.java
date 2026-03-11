package org.stockwellness.domain.portfolio.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.Portfolio;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorReport 도메인 단위 테스트")
class AdvisorReportTest {

    @Test
    @DisplayName("어드바이저 보고서를 생성할 수 있다")
    void create_advisor_report_success() {
        // given
        Portfolio portfolio = Portfolio.create(1L, "테스트 포트폴리오", "");
        String content = "AI 리밸런싱 조언 내용입니다.";
        AdviceAction action = AdviceAction.REBALANCE;

        // when
        AdvisorReport report = AdvisorReport.create(portfolio, content, action);

        // then
        assertThat(report.getPortfolio()).isEqualTo(portfolio);
        assertThat(report.getContent()).isEqualTo(content);
        assertThat(report.getAction()).isEqualTo(action);
    }
}
