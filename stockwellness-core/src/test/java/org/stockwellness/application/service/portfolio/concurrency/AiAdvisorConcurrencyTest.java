package org.stockwellness.application.service.portfolio.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.AiAdvisorService;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAdvisorService 동시성 테스트")
class AiAdvisorConcurrencyTest {

    @InjectMocks
    private AiAdvisorService aiAdvisorService;

    @Mock
    private PortfolioPort portfolioPort;

    @Mock
    private AdvisorAiDataLoader dataLoader;

    @Mock
    private AiAdviceProviderPort aiAdviceProviderPort;

    @Test
    @DisplayName("다수 사용자 요청 시 동시성 처리를 확인한다")
    void concurrent_advice_requests() {
        // given
        int requestCount = 10;
        Long memberId = 1L;
        Long portfolioId = 100L;
        Portfolio portfolio = Portfolio.create(memberId, "테스트", "");
        
        given(portfolioPort.loadPortfolio(portfolioId, memberId)).willReturn(Optional.of(portfolio));
        given(dataLoader.loadContext(portfolioId)).willReturn(mock(AdvisorAiContext.class));
        
        // 시뮬레이션: 각 AI 요청이 100ms 걸린다고 가정
        given(aiAdviceProviderPort.getRebalancingAdvice(any())).willAnswer(invocation -> {
            Thread.sleep(100);
            return new AiAdviceProviderPort.AdvisorAiResult("", "", "", "조언", AdviceAction.REBALANCE);
        });

        // when
        long start = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = IntStream.range(0, requestCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> aiAdvisorService.getNewAdvice(memberId, portfolioId)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();

        // then
        // 가상 스레드나 멀티스레드 환경에서 병렬로 처리되어야 함. 
        // 순차 처리 시 1000ms 이상 걸리겠지만, 병렬 처리 시 훨씬 단축되어야 함.
        System.out.println("Total time for " + requestCount + " concurrent requests: " + (end - start) + "ms");
        assertThat(end - start).isLessThan(1000); // 병렬 처리가 정상이라면 1초 이내 완료 기대
    }
}
