package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.AiAdvisorUseCase;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.LoadAdvisorPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAdvisorService implements AiAdvisorUseCase {

    private final PortfolioPort portfolioPort;
    private final AdvisorAiDataLoader dataLoader;
    private final AiAdviceProviderPort aiAdviceProviderPort;
    private final LoadAdvisorPort loadAdvisorPort;

    @Override
    @Transactional
    public AdviceResponse getNewAdvice(Long memberId, Long portfolioId) {
        validateOwnership(memberId, portfolioId);

        var context = dataLoader.loadContext(portfolioId);
        var aiResult = aiAdviceProviderPort.getRebalancingAdvice(context);

        return new AdviceResponse(
                aiResult.adviceContent(),
                aiResult.primaryAction(),
                LocalDateTime.now()
        );
    }

    @Override
    public AdviceResponse getLatestAdvice(Long memberId, Long portfolioId) {
        validateOwnership(memberId, portfolioId);

        return loadAdvisorPort.loadLatestReport(portfolioId)
                .map(report -> new AdviceResponse(
                        report.getContent(),
                        report.getAction(),
                        report.getCreatedAt()
                ))
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public String generateBacktestAdvice(BacktestResult result, String strategy, String benchmark) {
        BigDecimal cagr = result.cagr().multiply(BigDecimal.valueOf(100));
        BigDecimal mdd = result.mdd().multiply(BigDecimal.valueOf(100)).abs();
        BigDecimal sharpe = result.sharpeRatio();
        BigDecimal alpha = result.alpha();
        BigDecimal beta = result.beta();
        BigDecimal volatility = result.volatility();

        StringBuilder advice = new StringBuilder();
        advice.append(String.format("[투자 전략 분석] %s 전략 시뮬레이션 결과, ", strategy));
        advice.append(String.format("연평균 수익률(CAGR)은 %.2f%%, 최대 낙폭(MDD)은 %.2f%%, 샤프 지수는 %.2f를 기록했습니다. ",
                cagr, mdd, sharpe));

        // 1. 수익성 및 효율성 분석 (Sharpe, Alpha)
        if (sharpe.compareTo(BigDecimal.valueOf(1.5)) > 0) {
            advice.append("위험 대비 수익 효율이 매우 탁월한 포트폴리오입니다. ");
        } else if (sharpe.compareTo(BigDecimal.ONE) > 0) {
            advice.append("시장 평균보다 안정적인 성과를 유지하고 있습니다. ");
        } else {
            advice.append("수익 변동성이 다소 높으니 자산 구성을 재점검해보세요. ");
        }

        if (alpha.compareTo(BigDecimal.ZERO) > 0) {
            advice.append(String.format("벤치마크(%s) 대비 %.2f%%의 초과 수익(Alpha)을 달성하여 시장을 이기는 성과를 보여주었습니다. ", benchmark, alpha));
        }

        // 2. 위험 및 안정성 분석 (MDD, Volatility)
        if (mdd.compareTo(BigDecimal.valueOf(20)) > 0) {
            advice.append("하락장 방어력이 약하므로 채권이나 금 등 안전 자산 비중을 늘리는 것이 현명합니다. ");
        } else {
            advice.append("안정적인 하락장 방어력을 보여주어 장기 투자에 매우 유리한 구성입니다. ");
        }

        if (volatility.compareTo(BigDecimal.valueOf(20)) > 0) {
            advice.append("가격 변동성(Volatility)이 높은 편이므로 단기 변동에 유의하시기 바랍니다. ");
        }

        // 3. 시장 민감도 분석 (Beta)
        if (beta.compareTo(BigDecimal.valueOf(1.2)) > 0) {
            advice.append("시장 변화에 매우 민감하게 반응하는 공격적인 포트폴리오입니다. ");
        } else if (beta.compareTo(BigDecimal.valueOf(0.8)) < 0) {
            advice.append("시장 변동에 둔감하게 반응하는 방어적인 성격의 포트폴리오입니다. ");
        } else {
            advice.append("시장 지수와 유사한 흐름을 보이는 중립적인 구성입니다. ");
        }

        return advice.toString().trim();
    }

    private void validateOwnership(Long memberId, Long portfolioId) {
        portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });
    }
}
