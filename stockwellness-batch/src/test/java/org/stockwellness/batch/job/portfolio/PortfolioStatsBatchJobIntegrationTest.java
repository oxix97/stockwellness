package org.stockwellness.batch.job.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.stockwellness.adapter.out.persistence.member.MemberAdapter;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioAdapter;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.fixture.MemberFixture;
import org.stockwellness.support.BatchIntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioStatsBatchJob 통합 테스트")
class PortfolioStatsBatchJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("portfolioStatsJob")
    private Job job;

    @Autowired
    private PortfolioAdapter portfolioAdapter;

    @Autowired
    private MemberAdapter memberAdapter;

    @Test
    @DisplayName("포트폴리오 통계 계산 배치가 성공적으로 실행된다")
    void portfolioStatsJob_Success() throws Exception {
        // 준비
        Member member = MemberFixture.createMember();
        member = memberAdapter.saveMember(member);

        Portfolio portfolio = Portfolio.create(member.getId(), "테스트 포트폴리오", "설명");
        portfolioAdapter.savePortfolio(portfolio);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 실행
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
