package org.stockwellness.domain.portfolio.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 구성 종목이나 정보가 변경되었을 때 발행되는 도메인 이벤트입니다.
 */
@Getter
@RequiredArgsConstructor
public class PortfolioUpdatedEvent {
    private final Long memberId;
    private final Long portfolioId;
}
