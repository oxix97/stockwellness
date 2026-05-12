package org.stockwellness.application.port.in.member.result;

import java.time.LocalDateTime;

import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.member.RiskLevel;

public record MemberResult(
        Long id,
        String email,
        String nickname,
        MemberRole role,
        RiskLevel riskLevel,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        PortfolioSummaryResult portfolioSummary
) {
    public static MemberResult from(Member member, PortfolioSummaryResult summary) {
        return new MemberResult(
                member.getId(),
                member.getEmail().getAddress(),
                member.getNickname(),
                member.getRole(),
                member.getRiskLevel(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getModifiedAt(),
                summary
        );
    }

    // 포트폴리오 확장을 위한 Placeholder DTO
    public record PortfolioSummaryResult(
            Long totalAssetAmount,
            Double totalReturnRate
    ) {
        public static PortfolioSummaryResult empty() {
            return new PortfolioSummaryResult(0L, 0.0);
        }
    }
}