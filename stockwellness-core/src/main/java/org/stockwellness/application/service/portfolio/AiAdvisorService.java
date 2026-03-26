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
        BigDecimal alpha = result.alpha().multiply(BigDecimal.valueOf(100));
        BigDecimal beta = result.beta();
        BigDecimal volatility = result.volatility().multiply(BigDecimal.valueOf(100));

        String strategyName = "LUMP_SUM".equals(strategy) ? "거액 적립(Lump-sum)" : "정기 적립(DCA)";
        StringBuilder advice = new StringBuilder();

        // [총평] 성과 요약
        advice.append(String.format("📊 [%s 전략 시뮬레이션 결과]\n", strategyName));
        advice.append(String.format("지난 기간 동안 연평균 %.2f%%의 수익률(CAGR)을 기록했으며, 최악의 시기에도 %.2f%%(MDD) 수준으로 자산을 방어했습니다. ",
                cagr, mdd));

        // 1. 수익성 및 지수 대비 성과 (Sharpe, Alpha)
        if (alpha.compareTo(BigDecimal.ZERO) > 0) {
            advice.append(String.format("특히 벤치마크(%s) 대비 연간 %.2f%%의 추가 수익(Alpha)을 창출하며 시장 지수를 압도하는 탁월한 종목 선택 능력을 보여주었습니다. ", benchmark, alpha));
        } else {
            advice.append(String.format("시장 지수(%s)와 유사하거나 소폭 낮은 성과를 보였으나, 장기적인 안정성을 확보하는 데 주력한 모습입니다. ", benchmark));
        }

        if (sharpe.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            advice.append("위험 한 단위당 얻는 수익이 매우 높은 '고효율 포트폴리오'로 평가됩니다. ");
        } else if (sharpe.compareTo(BigDecimal.ONE) >= 0) {
            advice.append("위험 대비 수익률이 양호하여 장기 투자 시 복리 효과를 극대화하기에 적합합니다. ");
        }

        // 2. 위험 및 안정성 분석 (MDD, Volatility)
        if (mdd.compareTo(BigDecimal.valueOf(25)) > 0) {
            advice.append("\n⚠️ 주의: 최대 낙폭이 큰 편이므로, 하락장에서의 심리적 압박이 클 수 있습니다. 변동성을 줄이기 위해 채권이나 현금성 자산 혼합을 고려해보세요. ");
        } else if (mdd.compareTo(BigDecimal.valueOf(10)) < 0) {
            advice.append("\n✅ 안정성: 하락장에서도 자산 가치 하락을 효과적으로 방어하며 매우 탄탄한 방어력을 입증했습니다. ");
        }

        // 3. 시장 민감도 및 변동성 (Beta, Volatility)
        if (beta.compareTo(BigDecimal.valueOf(1.2)) > 0) {
            advice.append(String.format("시장(%s)의 움직임보다 더 민감하게 반응하는 '공격적 성장형' 성향이 뚜렷합니다. 강세장에서 큰 수익이 기대되지만 약세장에서는 주의가 필요합니다. ", benchmark));
        } else if (beta.compareTo(BigDecimal.valueOf(0.8)) < 0) {
            advice.append(String.format("시장(%s) 변동에 둔감하게 반응하는 '저변동 방어형' 성향을 보입니다. 하락장에서 상대적인 수익률 방어 효과가 뛰어날 것입니다. ", benchmark));
        } else {
            advice.append(String.format("시장 지수(%s)와 유사한 흐름을 보이는 '시장 추종형' 성향을 가지고 있습니다. ", benchmark));
        }

        if (volatility.compareTo(BigDecimal.valueOf(30)) > 0) {
            advice.append("자산의 가격 변동폭이 매우 크므로, 목표 수익 도달 시 부분 수익 실현을 권장합니다.");
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
