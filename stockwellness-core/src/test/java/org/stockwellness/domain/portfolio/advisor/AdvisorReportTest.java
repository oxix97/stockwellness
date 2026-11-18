package org.stockwellness.domain.portfolio.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorReport 도메인 단위 테스트")
class AdvisorReportTest {

    @Test
    @DisplayName("어드바이저 보고서를 생성할 수 있다")
    void create_advisor_report_success() {
        // given
        String content = "AI 리밸런싱 조언 내용입니다.";
        AdviceAction action = AdviceAction.REBALANCE;

        // when
        AdvisorReport report = AdvisorReport.create(1L, "gpt-4o-mini", "unversioned", "abc123", content, action);

        // then
        assertThat(report.getPortfolioId()).isEqualTo(1L);
        assertThat(report.getContent()).isEqualTo(content);
        assertThat(report.getAction()).isEqualTo(action);
        assertThat(report.getInputManifest()).containsEntry("context_sha256", "abc123");
        assertThat(report.getGeneratedAt()).isNotNull();
    }
}
