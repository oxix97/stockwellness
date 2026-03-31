package org.stockwellness.application.service.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.application.port.in.member.command.UpdateNotificationCommand;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.application.port.in.member.result.NotificationSettingsResult;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.exception.MemberNotFoundException;
import org.stockwellness.domain.member.exception.NicknameDuplicateException;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.error.exception.GlobalException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.stockwellness.global.error.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MemberService implements MemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final PortfolioPort portfolioPort;
    private final StockPricePort stockPricePort;

    @Override
    public MemberResult getMember(Long memberId) {
        var member = findMember(memberId);
        List<Portfolio> portfolios = portfolioPort.loadAllPortfolios(memberId);
        
        BigDecimal totalPurchaseAmount = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        for (Portfolio portfolio : portfolios) {
            totalPurchaseAmount = totalPurchaseAmount.add(portfolio.calculateTotalPurchaseAmount());
            
            // 각 포트폴리오별 실시간 시세 반영 (최적화 여지 있으나 현재는 직관적으로 구현)
            Map<String, BigDecimal> latestPrices = portfolio.getItems().stream()
                    .map(PortfolioItem::getSymbol)
                    .distinct()
                    .collect(Collectors.toMap(
                            symbol -> symbol,
                            symbol -> stockPricePort.findLatestByTicker(symbol)
                                    .map(StockPrice::getClosePrice)
                                    .orElse(BigDecimal.ZERO)
                    ));

            for (PortfolioItem item : portfolio.getItems()) {
                BigDecimal currentPrice = latestPrices.getOrDefault(item.getSymbol(), BigDecimal.ZERO);
                totalCurrentValue = totalCurrentValue.add(currentPrice.multiply(item.getQuantity()));
            }
        }

        Double totalReturnRate = 0.0;
        if (totalPurchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnRate = totalCurrentValue.subtract(totalPurchaseAmount)
                    .divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        MemberResult.PortfolioSummaryResult summary = new MemberResult.PortfolioSummaryResult(
                totalCurrentValue.longValue(),
                totalReturnRate
        );

        return MemberResult.from(member, summary);
    }

    @Override
    public void updateMember(Long memberId, UpdateMemberCommand command) {
        var member = findMember(memberId);

        if (!member.isActive()) {
            throw new GlobalException(UNAUTHORIZED);
        }

        if (command.nickname() != null && !member.getNickname().equals(command.nickname())) {
            if (loadMemberPort.existsByNickname(command.nickname())) {
                throw new NicknameDuplicateException();
            }
        }
        member.update(command.nickname(), command.riskLevel());
    }

    @Override
    public void withdrawMember(Long memberId) {
        var member = findMember(memberId);
        member.deactivate();
    }

    @Override
    public NotificationSettingsResult getNotificationSettings(Long memberId) {
        var member = findMember(memberId);
        return NotificationSettingsResult.from(member);
    }

    @Override
    public void updateNotificationSettings(Long memberId, UpdateNotificationCommand command) {
        var member = findMember(memberId);
        if (!member.isActive()) {
            throw new GlobalException(UNAUTHORIZED);
        }
        member.updateNotifications(command.rebalancing(), command.marketAlert(), command.newListing());
    }

    private Member findMember(Long memberId) {
        return loadMemberPort.loadMember(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
